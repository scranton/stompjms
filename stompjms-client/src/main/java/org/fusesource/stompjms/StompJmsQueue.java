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

import javax.jms.Queue;

/**
 * Queue implementation
 */
public class StompJmsQueue extends StompJmsDestination implements Queue {

    /**
     * Constructor
     *
     * @param name
     */
    public StompJmsQueue(String name) {
        super(name);
    }

    /**
     * @return name
     * @see javax.jms.Queue#getQueueName()
     */
    public String getQueueName() {
        return getPhysicalName();
    }

    protected String getType() {
        return StompJmsDestination.QUEUE_QUALIFIED_PREFIX;
    }
}
