/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stompjms;

import javax.jms.IllegalStateException;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

/**
 * Implementation of a TopicSubscriber
 */
public class StompJmsTopicSubscriber extends StompJmsMessageConsumer implements TopicSubscriber {
    private boolean noLocal;

    /**
     * Constructor
     *
     * @param s
     * @param destination
     */
    protected StompJmsTopicSubscriber(String id, StompJmsSession s, StompJmsDestination destination,
                                      boolean noLocal, String selector) {
        super(id, s, destination, selector);
        this.noLocal = noLocal;
    }

    /**
     * @return noLocak flag
     * @throws IllegalStateException
     * @see javax.jms.TopicSubscriber#getNoLocal()
     */
    public boolean getNoLocal() throws IllegalStateException {
        checkClosed();
        return this.noLocal;
    }

    /**
     * @return the Topic
     * @throws IllegalStateException
     * @see javax.jms.TopicSubscriber#getTopic()
     */
    public Topic getTopic() throws IllegalStateException {
        checkClosed();
        return (Topic) this.destination;
    }
}
