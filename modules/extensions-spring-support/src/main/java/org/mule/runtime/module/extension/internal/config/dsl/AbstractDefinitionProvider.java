/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.metadata.java.utils.JavaTypeUtils.getGenericTypeAt;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.metadata.utils.MetadataTypeUtils.getSingleAnnotation;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.annotation.GenericTypesAnnotation;
import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.TemplateParser;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.internal.introspection.BasicTypeMetadataVisitor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionFunctionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RegistryLookupValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.function.Function;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

public abstract class AbstractDefinitionProvider
{

    private final TemplateParser parser = TemplateParser.createMuleStyleParser();
    private final ConversionService conversionService = new DefaultConversionService();
    private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();


    protected final ComponentBuildingDefinition.Builder definition;

    protected AbstractDefinitionProvider(ComponentBuildingDefinition.Builder definition)
    {
        this.definition = definition.copy();
    }

    public abstract ComponentBuildingDefinition parse();

    private ParsedParameter parameterOf(String name, MetadataType expectedType, Object value, Object defaultValue)
    {
        ValueResolver resolver = null;
        if (isExpressionFunction(expectedType) && value != null)
        {
            resolver = new ExpressionFunctionValueResolver<>((String) value, getGenericTypeAt((ObjectType) expectedType, 1, typeLoader).get());
        }

        final Class<Object> expectedClass = getType(expectedType);
        if (resolver == null)
        {
            if (isExpression(value, parser))
            {
                resolver = new TypeSafeExpressionValueResolver((String) value, expectedClass);
            }
        }

        if (resolver == null && value != null)
        {
            final ValueHolder<ValueResolver> resolverValueHolder = new ValueHolder<>();
            expectedType.accept(new BasicTypeMetadataVisitor()
            {
                @Override
                protected void visitBasicType(MetadataType metadataType)
                {
                    if (conversionService.canConvert(value.getClass(), expectedClass))
                    {
                        resolverValueHolder.set(new StaticValueResolver(conversionService.convert(value, expectedClass)));
                    }
                    else
                    {
                        defaultVisit(metadataType);
                    }
                }

                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    resolverValueHolder.set(new RegistryLookupValueResolver(value.toString()));
                }
            });

            resolver = resolverValueHolder.get();
        }

        if (resolver == null)
        {
            resolver = new StaticValueResolver<>(defaultValue);
        }

        return new ParsedParameter(name, expectedType, resolver);
    }

    private boolean isExpression(Object value, TemplateParser parser)
    {
        return value instanceof String && parser.isContainsTemplate((String) value);
    }

    protected AttributeDefinition parseSimpleParameter(String name, MetadataType type, Object defaultValue)
    {
        return fromSimpleParameter(name, value -> parameterOf(name, type, value, defaultValue)).build();
    }

    protected void addResolver(ComponentBuildingDefinition.Builder definitionBuilder, AttributeDefinition attributeDefinition)
    {
        definitionBuilder.withSetterParameterDefinition("resolver", attributeDefinition);
    }

    //private TypeConverter getValueResolver(ParameterModel parameterModel, Object value)
    //{
    //    if (parameterModel.getExpressionSupport() == LITERAL)
    //    {
    //        return v -> new StaticValueResolver<>(value);
    //    }
    //
    //    ValueResolver<?> resolver = parseParameter(element, parameterModel);
    //    return resolver == null ? new StaticValueResolver(null) : resolver;
    //}

    private boolean isExpressionFunction(MetadataType metadataType)
    {
        if (!Function.class.isAssignableFrom(getType(metadataType)))
        {
            return false;
        }

        GenericTypesAnnotation generics = getSingleAnnotation(metadataType, GenericTypesAnnotation.class).orElse(null);
        if (generics == null)
        {
            return false;
        }

        if (generics.getGenericTypes().size() != 2)
        {
            return false;
        }

        final String genericClassName = generics.getGenericTypes().get(0);
        try
        {
            return MuleEvent.class.isAssignableFrom(ClassUtils.getClass(genericClassName));
        }
        catch (ClassNotFoundException e)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not load class " + genericClassName), e);
        }
    }
}

