/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.internal.stream;

import org.mule.runtime.core.api.MuleRuntimeException;

import java.io.InputStream;

public final class SocketInputStream extends AbstractSocketInputStream
{

    public SocketInputStream(final InputStream inputStream)
    {
        super(new LazyStreamSupplier(() -> {
            try
            {
                return inputStream;
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(e);
            }
        }));
    }

}
