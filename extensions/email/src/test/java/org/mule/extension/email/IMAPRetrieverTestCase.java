/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email;

import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.SEEN;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.extension.email.internal.EmailAttributes;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;

import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.Test;

public class IMAPRetrieverTestCase extends AbstractEmailRetrieverTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "imap.xml";
    }

    @Test
    public void retrieveAndRead() throws Exception
    {
        deliver10To(JUANI_EMAIL);
        MuleEvent event = runFlow(RETRIEVE_AND_READ);
        List<MuleMessage> messages = (List<MuleMessage>) event.getMessage().getPayload();
        assertThat(messages, hasSize(10));

        messages.forEach(m -> {
            assertThat(m.getPayload(), instanceOf(String.class));
            assertThat(m.getPayload(), is(CONTENT));
            assertThat(((EmailAttributes) m.getAttributes()).getFlags().isSeen(), is(true));
        });
    }

    @Test
    public void retrieveAndDontRead() throws Exception
    {
        deliver10To(JUANI_EMAIL);
        MuleEvent event = runFlow(RETRIEVE_AND_DONT_READ);
        List<MuleMessage> messages = (List<MuleMessage>) event.getMessage().getPayload();
        assertThat(messages, hasSize(10));
        messages.forEach(m -> assertThat(((EmailAttributes) m.getAttributes()).getFlags().isSeen(), is(false)));
    }

    @Test
    public void retrieveAndThenRead() throws Exception
    {
        deliver10To(JUANI_EMAIL);
        runFlow(RETRIEVE_AND_MARK_AS_READ);
        MimeMessage[] messages = server.getReceivedMessages();
        assertThat(messages.length, is(10));
        for (MimeMessage message : messages)
        {
            assertThat(message.getFlags().contains(SEEN), is(true));
        }
    }


    //@Test
    //public void retrieveAndExpungeDelete() throws Exception
    //{
    //    deliver10To(JUANI_EMAIL);
    //
    //    for (MimeMessage m : server.getReceivedMessages())
    //    {
    //        assertThat(m.getFlags().contains(DELETED), is(false));
    //    }
    //    runFlow(RETRIEVE_AND_THEN_EXPUNGE_DELETE);
    //    assertThat(server.getReceivedMessages().length, is(0));
    //}

    @Test
    public void retrieveAndMarkAsDelete() throws Exception
    {
        deliver10To(JUANI_EMAIL);

        for (MimeMessage m : server.getReceivedMessages())
        {
            assertThat(m.getFlags().contains(DELETED), is(false));
        }
        runFlow(RETRIEVE_AND_MARK_AS_DELETE);
        assertThat(server.getReceivedMessages().length, is(10));

        for (MimeMessage m : server.getReceivedMessages())
        {
            assertThat(m.getFlags().contains(DELETED), is(true));
        }
    }

    @Override
    public String getProtocol()
    {
        return "imap";
    }
}
