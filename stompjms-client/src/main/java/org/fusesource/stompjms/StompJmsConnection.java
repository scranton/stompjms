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

import org.fusesource.stompjms.channel.StompChannel;

import javax.jms.*;
import javax.jms.IllegalStateException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of a JMS Connection
 */
public class StompJmsConnection implements Connection, TopicConnection, QueueConnection {

    private String clientId;
    private int clientNumber = 0;
    private boolean clientIdSet;
    private ExceptionListener exceptionListener;
    private List<StompJmsSession> sessions = new CopyOnWriteArrayList<StompJmsSession>();
    private AtomicBoolean connected = new AtomicBoolean();
    private StompChannel mainChannel;
    private Map<StompJmsSession, StompChannel> channelsMap = new ConcurrentHashMap<StompJmsSession, StompChannel>();
    private AtomicBoolean closed = new AtomicBoolean();
    private AtomicBoolean started = new AtomicBoolean();

    /**
     * @param brokerURI
     * @param localURI
     * @param userName
     * @param password
     * @throws JMSException
     */
    protected StompJmsConnection(URI brokerURI, URI localURI, String userName, String password) throws JMSException {
        mainChannel = new StompChannel();
        mainChannel.setBrokerURI(brokerURI);
        mainChannel.setLocalURI(localURI);
        mainChannel.setUserName(userName);
        mainChannel.setPassword(password);
        mainChannel.setExceptionListener(this.exceptionListener);
        mainChannel.initialize();
    }

    /**
     * @throws JMSException
     * @see javax.jms.Connection#close()
     */
    public synchronized void close() throws JMSException {
        if (closed.compareAndSet(false, true)) {
            try {
                for (Session s : this.sessions) {
                    s.close();
                }
                this.sessions.clear();
                for (StompChannel c : channelsMap.values()) {
                    c.stop();
                }
                channelsMap.clear();
                if (mainChannel != null) {
                    mainChannel.stop();
                    mainChannel = null;
                }
            } catch (Exception e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    /**
     * @param destination
     * @param messageSelector
     * @param sessionPool
     * @param maxMessages
     * @return ConnectionConsumer
     * @throws JMSException
     * @see javax.jms.Connection#createConnectionConsumer(javax.jms.Destination,
     *      java.lang.String, javax.jms.ServerSessionPool, int)
     */
    public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector,
                                                       ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        checkClosed();
        connect();
        throw new JMSException("Not supported");
    }

    /**
     * @param topic
     * @param subscriptionName
     * @param messageSelector
     * @param sessionPool
     * @param maxMessages
     * @return ConnectionConsumer
     * @throws JMSException
     * @see javax.jms.Connection#createDurableConnectionConsumer(javax.jms.Topic,
     *      java.lang.String, java.lang.String, javax.jms.ServerSessionPool,
     *      int)
     */
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
                                                              String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        checkClosed();
        connect();
        throw new JMSException("Not supported");
    }

    /**
     * @param transacted
     * @param acknowledgeMode
     * @return Session
     * @throws JMSException
     * @see javax.jms.Connection#createSession(boolean, int)
     */
    public synchronized Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        checkClosed();
        connect();
        int ackMode = getSessionAcknowledgeMode(transacted, acknowledgeMode);
        StompChannel c = getChannel();
        StompJmsSession result = new StompJmsSession(this, c, ackMode);
        addSession(result, c);
        if (started.get()) {
            result.start();
        }
        return result;
    }

    /**
     * @return clientId
     * @see javax.jms.Connection#getClientID()
     */
    public String getClientID() {
        return this.clientId;
    }

    /**
     * @return ExceptionListener
     * @see javax.jms.Connection#getExceptionListener()
     */
    public ExceptionListener getExceptionListener() {
        return this.exceptionListener;
    }

    /**
     * @return ConnectionMetaData
     * @see javax.jms.Connection#getMetaData()
     */
    public ConnectionMetaData getMetaData() {
        return StompJmsConnectionMetaData.INSTANCE;
    }

    /**
     * @param clientID
     * @throws JMSException
     * @see javax.jms.Connection#setClientID(java.lang.String)
     */
    public synchronized void setClientID(String clientID) throws JMSException {
        if (this.clientIdSet) {
            throw new IllegalStateException("The clientID has already been set");
        }
        if (clientID == null) {
            throw new IllegalStateException("Cannot have a null clientID");
        }
        this.clientId = clientID;
        this.clientIdSet = true;
    }

    /**
     * @param listener
     * @see javax.jms.Connection#setExceptionListener(javax.jms.ExceptionListener)
     */
    public void setExceptionListener(ExceptionListener listener) {
        this.exceptionListener = listener;
    }

    /**
     * @throws JMSException
     * @see javax.jms.Connection#start()
     */
    public void start() throws JMSException {
        checkClosed();
        connect();
        if (this.started.compareAndSet(false, true)) {
            try {
                for (StompJmsSession s : this.sessions) {
                    s.start();
                }
            } catch (Exception e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    /**
     * @throws JMSException
     * @see javax.jms.Connection#stop()
     */
    public void stop() throws JMSException {
        checkClosed();
        connect();
        if (this.started.compareAndSet(true, false)) {
            try {
                for (StompJmsSession s : this.sessions) {
                    s.stop();
                }
            } catch (Exception e) {
                throw StompJmsExceptionSupport.create(e);
            }
        }
    }

    /**
     * @param topic
     * @param messageSelector
     * @param sessionPool
     * @param maxMessages
     * @return ConnectionConsumer
     * @throws JMSException
     * @see javax.jms.TopicConnection#createConnectionConsumer(javax.jms.Topic,
     *      java.lang.String, javax.jms.ServerSessionPool, int)
     */
    public ConnectionConsumer createConnectionConsumer(Topic topic, String messageSelector,
                                                       ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        checkClosed();
        connect();
        return null;
    }

    /**
     * @param transacted
     * @param acknowledgeMode
     * @return TopicSession
     * @throws JMSException
     * @see javax.jms.TopicConnection#createTopicSession(boolean, int)
     */
    public TopicSession createTopicSession(boolean transacted, int acknowledgeMode) throws JMSException {
        checkClosed();
        connect();
        int ackMode = getSessionAcknowledgeMode(transacted, acknowledgeMode);
        StompChannel c = getChannel();
        StompJmsTopicSession result = new StompJmsTopicSession(this, c, ackMode);
        addSession(result, c);
        if (started.get()) {
            result.start();
        }
        return result;
    }

    /**
     * @param queue
     * @param messageSelector
     * @param sessionPool
     * @param maxMessages
     * @return ConnectionConsumer
     * @throws JMSException
     * @see javax.jms.QueueConnection#createConnectionConsumer(javax.jms.Queue,
     *      java.lang.String, javax.jms.ServerSessionPool, int)
     */
    public ConnectionConsumer createConnectionConsumer(Queue queue, String messageSelector,
                                                       ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        checkClosed();
        connect();
        return null;
    }

    /**
     * @param transacted
     * @param acknowledgeMode
     * @return QueueSession
     * @throws JMSException
     * @see javax.jms.QueueConnection#createQueueSession(boolean, int)
     */
    public QueueSession createQueueSession(boolean transacted, int acknowledgeMode) throws JMSException {
        checkClosed();
        connect();
        int ackMode = getSessionAcknowledgeMode(transacted, acknowledgeMode);
        StompChannel c = getChannel();
        StompJmsQueueSession result = new StompJmsQueueSession(this, c, ackMode);
        addSession(result, c);
        if (started.get()) {
            result.start();
        }
        return result;
    }

    /**
     * @param ex
     */
    public void onException(Exception ex) {
        onException(StompJmsExceptionSupport.create(ex));
    }

    /**
     * @param ex
     */
    public void onException(JMSException ex) {
        ExceptionListener l = this.exceptionListener;
        if (l != null) {
            l.onException(StompJmsExceptionSupport.create(ex));
        }
    }

    protected int getSessionAcknowledgeMode(boolean transacted, int acknowledgeMode) throws JMSException {
        int result = acknowledgeMode;
        if (!transacted && acknowledgeMode == Session.SESSION_TRANSACTED) {
            throw new JMSException("acknowledgeMode SESSION_TRANSACTED cannot be used for an non-transacted Session");
        }
        if (transacted) {
            result = Session.SESSION_TRANSACTED;
        }
        return result;
    }

    protected synchronized StompChannel getChannel() throws JMSException {
        StompChannel result = null;
        if (this.channelsMap.isEmpty()) {
            result = this.mainChannel;
        } else {
            result = this.mainChannel.copy();
            result.setExceptionListener(this.exceptionListener);
            result.setChannelId(clientId + "-" + clientNumber++);
            result.initialize();
            result.connect();
            result.start();
        }
        return result;
    }

    protected synchronized void removeSession(StompJmsSession s) throws JMSException {
        this.sessions.remove(s);
        StompChannel c = this.channelsMap.remove(s);
        if (c != null) {

            c.setListener(null);
            if (c != this.mainChannel) {
                c.setExceptionListener(null);
                c.stop();
            }
        }
    }

    protected void addSession(StompJmsSession s, StompChannel c) {
        this.sessions.add(s);
        this.channelsMap.put(s, c);
    }

    protected void checkClosed() throws IllegalStateException {
        if (this.closed.get()) {
            throw new IllegalStateException("The MessageProducer is closed");
        }
    }

    private void connect() throws JMSException {
        if (connected.compareAndSet(false, true)) {
            this.mainChannel.connect();
            this.mainChannel.start();
            for (StompChannel sc : this.channelsMap.values()) {
                sc.connect();
                sc.start();
            }
        }
    }
}
