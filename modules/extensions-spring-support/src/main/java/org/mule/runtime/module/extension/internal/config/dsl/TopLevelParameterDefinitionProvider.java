/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromType;
import static org.mule.runtime.module.extension.internal.util.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.utils.JavaTypeUtils;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.processor.TypeDefinition;

final class TopLevelParameterDefinitionProvider extends AbstractDefinitionProvider
{

    private final ObjectType type;

    TopLevelParameterDefinitionProvider(ComponentBuildingDefinition.Builder definition, ObjectType type)
    {
        super(definition);
        this.type = type;
    }

    public void parse() {

        definition.withIdentifier(hyphenize(getTopLevelTypeName(type)))
                .withTypeDefinition(fromType(getType(type)))
                .with
    }
}
