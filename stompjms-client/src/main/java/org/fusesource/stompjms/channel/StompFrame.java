/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms.channel;


import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents all the data in a STOMP frame.
 *
 * @author <a href="http://hiramchirino.com">chirino</a>
 */
public class StompFrame {

    public static final Buffer NO_DATA = new Buffer(new byte[]{});

    private String action;
    private Map<String, String> headers = new HashMap<String, String>();
    private Buffer content = NO_DATA;

    public StompFrame(String command) {
        this(command, null, (Buffer) null);
    }

    public StompFrame(String command, Map<String, String> headers) {
        this(command, headers, (Buffer) null);
    }

    public StompFrame(String command, Map<String, String> headers, Buffer data) {
        this.action = command;
        if (headers != null)
            this.headers = headers;
        if (data != null)
            this.content = data;
    }

    public StompFrame(String command, Map<String, String> headers, byte[] data) {
        this(command, headers, (data != null ? new Buffer(data) : null));
    }

    public StompFrame() {
    }

    public String getAction() {
        return action;
    }

    public void setAction(String command) {
        this.action = command;
    }

    public Buffer getContent() {
        return this.content;
    }

    public String getBody() {
        try {
            Buffer b = getContent();
            if (b != null) {
                return new String(b.getData(), b.getOffset(), b.getLength(), "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
        }
        return new String("");
    }

    public void setContent(Buffer data) {
        this.content = data;
    }

    public void clearContent() {
        this.content = NO_DATA;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }


    public Buffer toBuffer() throws IOException {

        DataByteArrayOutputStream out = new DataByteArrayOutputStream();
        StringBuffer buffer = new StringBuffer();

        buffer.append(getAction());
        buffer.append(Stomp.NEWLINE);

        Map<String, String> h = getHeaders();
        for (Iterator<Map.Entry<String, String>> iter = h.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, String> entry = iter.next();
            buffer.append(entry.getKey());
            buffer.append(":");
            buffer.append(entry.getValue());
            buffer.append(Stomp.NEWLINE);
        }
        //denotes end of headers with a new line
        buffer.append(Stomp.Headers.CONTENT_LENGTH).append(":");
        Buffer b = getContent();
        int contentLength = b != null ? b.length() : 0;
        buffer.append(contentLength);

        if (b != null) {
            buffer.append(Stomp.NEWLINE);
            buffer.append(Stomp.NEWLINE);
            String content = new String(b.getData(), b.getOffset(), b.getLength(), "UTF-8");
            buffer.append(content);
        }
        buffer.append(Stomp.NULL);
        out.write(buffer.toString().getBytes());
        Buffer result = out.toBuffer();
        return result;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(getAction());
        buffer.append(Stomp.NEWLINE);

        Map<String, String> h = getHeaders();
        for (Iterator<Map.Entry<String, String>> iter = h.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, String> entry = iter.next();
            buffer.append(entry.getKey());
            buffer.append(":");
            buffer.append(entry.getValue());
            buffer.append(Stomp.NEWLINE);
        }
        //denotes end of headers with a new line
        buffer.append(Stomp.NEWLINE);
        Buffer b = getContent();
        if (b != null) {

            String content = null;
            try {
                content = new String(b.getData(), b.getOffset(), b.getLength(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            buffer.append(Stomp.Headers.CONTENT_LENGTH);
            buffer.append(":").append(content.length());
            buffer.append(Stomp.NEWLINE);
            buffer.append(content);
        }
        return buffer.toString();
    }
}
