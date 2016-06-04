/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.sender;

import static org.mule.extension.email.internal.util.EmailConstants.PORT_SMTP;
import static org.mule.extension.email.internal.util.EmailConstants.PROTOCOL_SMTP;
import org.mule.extension.email.api.AbstractEmailProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * A {@link ConnectionProvider} that returns instances of smtp based {@link SenderConnection}s.
 *
 * @since 4.0
 */
@Alias("smtp")
public class SMTPConnectionProvider extends AbstractEmailProvider implements ConnectionProvider<SMTPConfiguration, SenderConnection>
{

    /**
     * the port number of the mail server.
     */
    @Parameter
    @Optional(defaultValue = PORT_SMTP)
    protected String port;

    /**
     * the username used to connect with the mail server.
     */
    @Parameter
    @Optional
    protected String user;

    /**
     * the password corresponding to the {@code username}.
     */
    @Parameter
    @Optional
    protected String password;

    /**
     * {@inheritDoc}
     */
    @Override
    public SenderConnection connect(SMTPConfiguration config) throws ConnectionException
    {
        return new SenderConnection(PROTOCOL_SMTP, user, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect(SenderConnection connection)
    {
        connection.disconnect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionValidationResult validate(SenderConnection connection)
    {
        return connection.validate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<SenderConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<SMTPConfiguration, SenderConnection> handlingStrategyFactory)
    {
        return handlingStrategyFactory.supportsPooling();
    }
}
