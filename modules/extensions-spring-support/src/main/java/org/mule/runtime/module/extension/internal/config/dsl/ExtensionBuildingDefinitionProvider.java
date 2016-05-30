/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.runtime.config.spring.dsl.processor.xml.CoreXmlNamespaceInfoProvider.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.Preconditions.checkState;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.config.spring.MuleArtifactContext;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.BaseExtensionWalker;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import org.mule.runtime.module.extension.internal.config.TopLevelParameterTypeBeanDefinitionParser;
import org.mule.runtime.module.extension.internal.introspection.SubTypesMappingContainer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

public class ExtensionBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider
{
    private final List<ComponentBuildingDefinition> definitions = new LinkedList<>();

    private final Map<String, ExtensionModel> handledExtensions = new HashMap<>();
    private final Multimap<ExtensionModel, String> topLevelParameters = HashMultimap.create();
    private final Map<String, BeanDefinitionParser> parsers = new HashMap<>();
    private ExtensionManager extensionManager;

    /**
     * Attempts to get a hold on a {@link ExtensionManager}
     * instance
     *
     * @throws java.lang.IllegalStateException if no extension manager could be found
     */
    @Override
    public void init(MuleContext muleContext)
    {
        extensionManager = MuleArtifactContext.getCurrentMuleContext().get().getExtensionManager();
        checkState(extensionManager != null, "Could not obtain the ExtensionManager");

        extensionManager.getExtensions().forEach(this::registerExtensionParsers);

    }

    @Override
    public List<ComponentBuildingDefinition> getComponentBuildingDefinitions()
    {
        return null;
    }

    private void registerExtensionParsers(ExtensionModel extensionModel) {
        XmlModelProperty xmlModelProperty = extensionModel.getModelProperty(XmlModelProperty.class).orElse(null);
        if (xmlModelProperty == null) {
            return;
        }

        final ComponentBuildingDefinition.Builder baseExtensionDefinition = new ComponentBuildingDefinition.Builder().withNamespace(xmlModelProperty.getNamespace());
        withContextClassLoader(getClassLoader(extensionModel), () -> {

            registerTopLevelParameters(extensionModel, baseExtensionDefinition.copy());
            registerConfigurations(extensionModel);
            registerOperations(extensionModel, extensionModel.getOperationModels());
            registerConnectionProviders(extensionModel, extensionModel.getConnectionProviders());
            registerMessageSources(extensionModel, extensionModel.getSourceModels());

            handledExtensions.put(xmlModelProperty.getNamespace(), extensionModel);
        });
    }

    private void registerTopLevelParameters(ExtensionModel extensionModel, ComponentBuildingDefinition.Builder definition)
    {
        Optional<SubTypesModelProperty> subTypesProperty = extensionModel.getModelProperty(SubTypesModelProperty.class);
        SubTypesMappingContainer typeMapping = new SubTypesMappingContainer(subTypesProperty.isPresent() ? subTypesProperty.get().getSubTypesMapping() : Collections.emptyMap());

        new BaseExtensionWalker() {

            @Override
            public void onParameter(ParameterizedModel owner, ParameterModel model)
            {
                typeMapping.getSubTypes(model.getType()).forEach(subtype -> registerTopLevelParameter(extensionModel, subtype, definition));
                registerTopLevelParameter(extensionModel, model.getType(), definition);
            }
        }.walk(extensionModel);
    }

    private void registerTopLevelParameter(final ExtensionModel extensionModel, final MetadataType parameterType, ComponentBuildingDefinition.Builder definition)
    {
        parameterType.accept(new MetadataTypeVisitor()
        {
            @Override
            public void visitObject(ObjectType objectType)
            {
                String name = hyphenize(getTopLevelTypeName(objectType));
                if (topLevelParameters.put(extensionModel, name))
                {
                    new TopLevelParameterDefinitionProvider(definition, objectType);
                }
            }

            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                registerTopLevelParameter(extensionModel, arrayType.getType(), definition.copy());
            }

            @Override
            public void visitDictionary(DictionaryType dictionaryType)
            {
                MetadataType keyType = dictionaryType.getKeyType();
                keyType.accept(this);
                registerTopLevelParameter(extensionModel, keyType, definition.copy());
            }
        });
    }

}
