/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.ssl;

import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.transport.AbstractMessageReceiverTestCase;
import org.mule.runtime.transport.ssl.SslMessageReceiver;

public class SslMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    @Override
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Connector connector = endpoint.getConnector();
        return new SslMessageReceiver(connector, mock(Flow.class), endpoint);
    }

    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
        return getEndpointFactory().getInboundEndpoint("ssl://localhost:1234");
    }
}
