/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.runtime.module.extension.internal.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

final class ConfigurationProviderObjectFactory implements ObjectFactory<ConfigurationProvider>
{
    private final String name;
    private final ExtensionModel extensionModel;
    private final RuntimeConfigurationModel configurationModel;
    private final ConfigurationProviderFactory configurationProviderFactory = new DefaultConfigurationProviderFactory();
    private final TimeSupplier timeSupplier;
    private final ValueResolver<ConnectionProvider> connectionProviderResolver;
    private final MuleContext muleContext;
    private DynamicConfigPolicy dynamicConfigPolicy;

    ConfigurationProviderObjectFactory(String name,
                                     RuntimeConfigurationModel configurationModel,
                                     MuleContext muleContext,
                                     TimeSupplier timeSupplier,
                                     ValueResolver<ConnectionProvider> connectionProviderResolver) throws ConfigurationException
    {
        this.name = name;
        this.configurationModel = configurationModel;
        this.connectionProviderResolver = connectionProviderResolver;
        this.timeSupplier = timeSupplier;
        this.muleContext = muleContext;
        this.extensionModel = configurationModel.getExtensionModel();
    }

    @Override
    public ConfigurationProvider<Object> getObject() throws Exception
    {
        //ResolverSet resolverSet = parserDelegate.getResolverSet(element, configurationModel.getParameterModels());
        //ConfigurationProvider<Object> configurationProvider;
        //try
        //{
        //    if (resolverSet.isDynamic() || connectionProviderResolver.isDynamic())
        //    {
        //        configurationProvider = configurationProviderFactory.createDynamicConfigurationProvider(
        //                name,
        //                configurationModel,
        //                resolverSet,
        //                connectionProviderResolver,
        //                getDynamicConfigPolicy(element));
        //    }
        //    else
        //    {
        //        configurationProvider = configurationProviderFactory.createStaticConfigurationProvider(
        //                name,
        //                configurationModel,
        //                resolverSet,
        //                connectionProviderResolver,
        //                muleContext);
        //    }
        //
        //    muleContext.getInjector().inject(configurationProvider);
        //}
        //catch (Exception e)
        //{
        //    throw new RuntimeException(e);
        //}
        //return configurationProvider;

        return null;
    }

    public void setDynamicConfigPolicy(DynamicConfigPolicy dynamicConfigPolicy)
    {
        this.dynamicConfigPolicy = dynamicConfigPolicy;
    }
}
