/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;

abstract class AbstractDefinitionProvider
{
    protected final ComponentBuildingDefinition.Builder definition;

    AbstractDefinitionProvider(ComponentBuildingDefinition.Builder definition)
    {
        this.definition = definition.copy();
    }
}
