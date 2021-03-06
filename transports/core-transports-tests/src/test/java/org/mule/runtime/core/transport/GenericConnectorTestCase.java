/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;

import org.junit.Test;

/**
 * The test is not there in AbstractConnector, because we need to call a protected
 * method, and the latter class is in a different package.
 */
public class GenericConnectorTestCase extends AbstractMuleContextEndpointTestCase
{

    /**
     * Throwable should not cause a ClassCastException (MULE-802).
     * 
     * @throws Exception
     */
    @Test
    public void testSpiWorkThrowableHandling() throws Exception
    {
        try
        {
            AbstractConnector connector = getTestConnector();
            connector.handleWorkException(getTestWorkEvent(), "workRejected");
        }
        catch (MuleRuntimeException mrex)
        {
            assertNotNull(mrex);
            assertTrue(mrex.getCause().getClass() == Throwable.class);
            assertEquals("testThrowable", mrex.getCause().getMessage());
        }
    }

    private WorkEvent getTestWorkEvent()
    {
        WorkEvent event = new WorkEvent(this, // source
            WorkEvent.WORK_REJECTED, getTestWork(), new WorkException(new Throwable("testThrowable")));
        return event;
    }

    private Work getTestWork()
    {
        return new Work()
        {
            @Override
            public void release()
            {
                // noop
            }

            @Override
            public void run()
            {
                // noop
            }
        };
    }
}
