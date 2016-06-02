/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromType;
import static org.mule.runtime.module.extension.internal.util.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition.Builder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

final class TopLevelParameterParser extends AbstractDefinitionProvider
{

    private final ObjectType type;

    TopLevelParameterParser(Builder definition, ObjectType type)
    {
        super(definition);
        this.type = type;
    }

    @Override
    public ComponentBuildingDefinition parse()
    {
        Builder definitionBuilder = definition.withIdentifier(hyphenize(getTopLevelTypeName(type)))
                .withTypeDefinition(fromType(ValueResolver.class))
                .withObjectFactoryType(TopLevelParameterObjectFactory.class)
                .withSetterParameterDefinition("type", fromFixedValue(type).build());

        for (ObjectFieldType objectField : type.getFields())
        {
            final MetadataType fieldType = objectField.getValue();
            final String parameterName = objectField.getKey().getName().getLocalPart();

            fieldType.accept(new MetadataTypeVisitor()
            {

                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    addResolver(definitionBuilder, parseSimpleParameter(parameterName, metadataType, null));
                }

                @Override
                public void visitObject(ObjectType objectType)
                {
                    //addResolver(definitionBuilder, fromChildConfiguration(getType(objectType)).build());
                }
            });
        }

        return definitionBuilder.build();
    }
}
