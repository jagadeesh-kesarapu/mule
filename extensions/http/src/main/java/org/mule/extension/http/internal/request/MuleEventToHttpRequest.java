/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.COOKIE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.runtime.module.http.internal.request.DefaultHttpRequester.DEFAULT_PAYLOAD_EXPRESSION;
import org.mule.extension.http.api.HttpSendBodyMode;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.extension.http.api.request.HttpRequesterConfig;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.transformer.types.MimeTypes;
import org.mule.runtime.core.util.DataTypeUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.runtime.module.http.internal.domain.EmptyHttpEntity;
import org.mule.runtime.module.http.internal.domain.HttpEntity;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.MultipartHttpEntity;
import org.mule.runtime.module.http.internal.multipart.HttpPartDataSource;

import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleEventToHttpRequest
{
    private static final Logger logger = LoggerFactory.getLogger(MuleEventToHttpRequest.class);
    public static final List<String> DEFAULT_EMPTY_BODY_METHODS = Lists.newArrayList("GET", "HEAD", "OPTIONS");

    private final String uri;
    private final String method;
    private final HttpRequesterConfig config;
    private final HttpStreamingType streamingMode;
    private final HttpSendBodyMode sendBodyMode;
    private final String source;


    public MuleEventToHttpRequest(HttpRequesterConfig config, String uri, String method, HttpStreamingType streamingMode, HttpSendBodyMode sendBodyMode, String source)
    {
        this.config = config;
        this.uri = uri;
        this.method = method;
        this.streamingMode = streamingMode;
        this.sendBodyMode = sendBodyMode;
        this.source = source;
    }


    public HttpRequestBuilder create(MuleEvent event, HttpRequesterRequestBuilder requestBuilder) throws MuleException
    {
        HttpRequestBuilder builder = new HttpRequestBuilder();

        builder.setUri(this.uri);
        builder.setMethod(this.method);
        builder.setHeaders(requestBuilder.getHeaders());
        builder.setQueryParams(requestBuilder.getQueryParams());

        if (!builder.getHeaders().containsKey(MuleProperties.CONTENT_TYPE_PROPERTY))
        {
            DataType<?> dataType = event.getMessage().getDataType();
            if (!MimeTypes.ANY.equals(dataType.getMimeType()))
            {
                builder.addHeader(MuleProperties.CONTENT_TYPE_PROPERTY, DataTypeUtils.getContentType(dataType));
            }
        }

        if (config.isEnableCookies())
        {
            try
            {
                Map<String, List<String>> headers = config.getCookieManager().get(URI.create(uri),
                                                                                                 Collections.<String, List<String>>emptyMap());
                List<String> cookies = headers.get(COOKIE);
                if (cookies != null)
                {
                    for (String cookie : cookies)
                    {
                        builder.addHeader(COOKIE, cookie);
                    }
                }
            }
            catch (IOException e)
            {
                logger.warn("Error reading cookies for URI " + uri, e);
            }

        }

        builder.setEntity(createRequestEntity(builder, event, this.method, requestBuilder.getParts()));

        return builder;
    }

    private HttpEntity createRequestEntity(HttpRequestBuilder requestBuilder, MuleEvent muleEvent, String resolvedMethod, Map<String, DataHandler> parts) throws MessagingException
    {
        boolean customSource = false;
        Object oldPayload = null;
        HttpEntity entity;

        if (!StringUtils.isEmpty(this.source) && !(DEFAULT_PAYLOAD_EXPRESSION.equals(this.source)))
        {
            Object newPayload = this.source;
            oldPayload = muleEvent.getMessage().getPayload();
            muleEvent.getMessage().setPayload(newPayload);
            customSource = true;
        }

        if (isEmptyBody(muleEvent, resolvedMethod))
        {
            entity = new EmptyHttpEntity();
        }
        else
        {
            entity = createRequestEntityFromPayload(requestBuilder, muleEvent, parts);
        }

        if (customSource)
        {
            muleEvent.getMessage().setPayload(oldPayload);
        }

        return entity;
    }

    private boolean isEmptyBody(MuleEvent event, String method)
    {
        boolean emptyBody;

        if (event.getMessage().getPayload() instanceof NullPayload && event.getMessage().getOutboundAttachmentNames().isEmpty())
        {
            emptyBody = true;
        }
        else
        {
            emptyBody = DEFAULT_EMPTY_BODY_METHODS.contains(method);

            if (sendBodyMode != HttpSendBodyMode.AUTO)
            {
                emptyBody = (sendBodyMode == HttpSendBodyMode.NEVER);
            }
        }

        return emptyBody;
    }

    private HttpEntity createRequestEntityFromPayload(HttpRequestBuilder requestBuilder, MuleEvent muleEvent, Map<String, DataHandler> parts) throws MessagingException
    {
        Object payload = muleEvent.getMessage().getPayload();

        if (!parts.isEmpty())
        {
            try
            {
                return new MultipartHttpEntity(HttpPartDataSource.createFrom(parts));
            }
            catch (IOException e)
            {
                throw new MessagingException(muleEvent, e);
            }
        }

        if (doStreaming(requestBuilder, muleEvent))
        {

            if (payload instanceof InputStream)
            {
                return new InputStreamHttpEntity((InputStream) payload);
            }
            else
            {
                try
                {
                    return new InputStreamHttpEntity(new ByteArrayInputStream(muleEvent.getMessageAsBytes()));
                }
                catch (Exception e)
                {
                    throw new MessagingException(muleEvent, e);
                }
            }

        }
        else
        {
            String contentType = requestBuilder.getHeaders().get(CONTENT_TYPE);

            if (contentType == null || contentType.equals(APPLICATION_X_WWW_FORM_URLENCODED))
            {
                if (muleEvent.getMessage().getPayload() instanceof Map)
                {
                    String body = HttpParser.encodeString(muleEvent.getEncoding(), (Map) payload);
                    requestBuilder.addHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
                    return new ByteArrayHttpEntity(body.getBytes());
                }
            }

            try
            {
                return new ByteArrayHttpEntity(muleEvent.getMessageAsBytes());
            }
            catch (Exception e)
            {
                throw new MessagingException(muleEvent, e);
            }
        }
    }

    private boolean doStreaming(HttpRequestBuilder requestBuilder, MuleEvent event) throws MessagingException
    {
        String transferEncodingHeader = requestBuilder.getHeaders().get(TRANSFER_ENCODING);
        String contentLengthHeader = requestBuilder.getHeaders().get(CONTENT_LENGTH);

        Object payload = event.getMessage().getPayload();

        if (streamingMode == HttpStreamingType.AUTO)
        {
            if (contentLengthHeader != null)
            {
                if (transferEncodingHeader != null)
                {
                    requestBuilder.removeHeader(TRANSFER_ENCODING);

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Cannot send both Transfer-Encoding and Content-Length headers. Transfer-Encoding will not be sent.");
                    }
                }
                return false;
            }

            if (transferEncodingHeader == null || !transferEncodingHeader.equalsIgnoreCase(CHUNKED))
            {
                return payload instanceof InputStream;
            }
            else
            {
                return true;
            }
        }
        else if (streamingMode == HttpStreamingType.ALWAYS)
        {
            if (contentLengthHeader != null)
            {
                requestBuilder.removeHeader(CONTENT_LENGTH);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Content-Length header will not be sent, as the configured requestStreamingMode is ALWAYS");
                }
            }

            if (transferEncodingHeader != null && !transferEncodingHeader.equalsIgnoreCase(CHUNKED))
            {
                requestBuilder.removeHeader(TRANSFER_ENCODING);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Transfer-Encoding header will be sent with value 'chunked' instead of {}, as the configured " +
                                 "requestStreamingMode is NEVER", transferEncodingHeader);
                }

            }
            return true;
        }
        else
        {
            if (transferEncodingHeader != null)
            {
                requestBuilder.removeHeader(TRANSFER_ENCODING);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Transfer-Encoding header will not be sent, as the configured requestStreamingMode is NEVER");
                }
            }
            return false;
        }
    }
}
