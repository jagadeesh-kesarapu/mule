/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever.pop3;

import static org.mule.extension.email.internal.util.EmailConstants.PROTOCOL_POP3;
import org.mule.extension.email.api.retriever.AbstractRetrieverProvider;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;

/**
 * A {@link ConnectionProvider} that returns instances of pop3 based {@link RetrieverConnection}s.
 *
 * @since 4.0
 */
@Alias("pop3")
public class POP3Provider extends AbstractRetrieverProvider implements ConnectionProvider<POP3Configuration, RetrieverConnection>
{

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrieverConnection connect(POP3Configuration config) throws ConnectionException
    {
        return new RetrieverConnection(PROTOCOL_POP3,
                                       user,
                                       password,
                                       config.getHost(),
                                       config.getPort(),
                                       config.getConnectionTimeout(),
                                       config.getReadTimeout(),
                                       config.getWriteTimeout(),
                                       config.getProperties(),
                                       config.getFolder());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<RetrieverConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<POP3Configuration, RetrieverConnection> connectionHandlingStrategyFactory)
    {
        return connectionHandlingStrategyFactory.cached();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect(RetrieverConnection connection)
    {
        connection.disconnect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionValidationResult validate(RetrieverConnection connection)
    {
        return connection.validate();
    }
}
