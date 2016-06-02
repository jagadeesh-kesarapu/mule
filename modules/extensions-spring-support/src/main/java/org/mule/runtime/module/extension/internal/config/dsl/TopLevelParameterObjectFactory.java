/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldByAlias;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.module.extension.internal.runtime.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.ObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.lang.reflect.Field;

public class TopLevelParameterObjectFactory extends AbstractExtensionObjectFactory<ValueResolver<Object>>
{

    private ObjectType type;

    @Override
    public ValueResolver<Object> getObject() throws Exception
    {
        final Class<Object> objectClass = getType(type);
        final ObjectBuilder builder = new DefaultObjectBuilder(objectClass);

        getParsedParameters().forEach(parameter -> {
            Field field = getFieldByAlias(objectClass, parameter.getName(), getType(parameter.getType()));
            if (field != null)
            {
                builder.addPropertyResolver(field, parameter.getResolver());
            }
        });

        return new ObjectBuilderValueResolver<>(builder);
    }

    public void setType(ObjectType type)
    {
        this.type = type;
    }
}
