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

import javax.jms.Topic;

/**
 * TemporaryQueue
 */
public class StompJmsTopic extends StompJmsDestination implements Topic {
    /**
     * Constructor
     *
     * @param name
     */
    public StompJmsTopic(String name) {
        super(name);
        this.topic = true;
    }

    /**
     * @return the name
     * @see javax.jms.Topic#getTopicName()
     */
    public String getTopicName() {
        return getPhysicalName();
    }

    protected String getType() {
        return StompJmsDestination.TOPIC_QUALIFIED_PREFIX;
    }
}
