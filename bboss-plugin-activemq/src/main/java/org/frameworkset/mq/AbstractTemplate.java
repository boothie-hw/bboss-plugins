/*
 *  Copyright 2008 biaoping.yin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.frameworkset.mq;

import org.slf4j.Logger;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Title: AbstractTemplate.java
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * bboss workgroup
 * </p>
 * <p>
 * Copyright (c) 2007
 * </p>
 * 
 * @Date 2010-1-13 下午04:14:43
 * @author biaoping.yin
 * @version 1.0
 */
public abstract class AbstractTemplate implements org.frameworkset.spi.DisposableBean {
	// protected boolean transacted = false;

	// protected int destinationType = MQUtil.TYPE_QUEUE;

	// protected String requestMessageSelector;

	// protected String responseMessageSelector;
 

	protected ConnectionFactory connectionFactory;

	protected Connection connection;

	 

	// protected RequestDispatcher responseDispatcher;

	// protected String replyto;

	// protected int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

 
	protected List<ReceiveDispatcher> tempdispatcher = new ArrayList<ReceiveDispatcher>();

	public AbstractTemplate(JMSConnectionFactory connectionFactory) throws JMSException {
		this(connectionFactory.getConectionFactory());

	}

 
	public AbstractTemplate(ConnectionFactory connectionFactory) throws JMSException {
		 
		// this.replyto = replyto;

		// this.responseMessageSelector = responseMessageSelector;

		if (connectionFactory instanceof ConnectionFactoryWrapper)
			this.connectionFactory = connectionFactory;
		else
			this.connectionFactory = new ConnectionFactoryWrapper(connectionFactory, null);

		connection = this.connectionFactory.createConnection();	
		connection.start();
	}

	public void destroy() throws Exception {
		this.stop();

	}
	private boolean stoped;
	public void stop() {
		if(stoped)
			return;
		if (this.tempdispatcher.size() > 0) {
			for (ReceiveDispatcher dispatcher : this.tempdispatcher) {
				try {
					dispatcher.stop();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		// if(this.responseDispatcher != null)
		// {
		// this.responseDispatcher.stop();
		// }
		if (this.connection != null)
			try {
				connection.stop();

			} catch (Exception e) {
				e.printStackTrace();
			}
		// try
		// {
		//
		// connection.setClientID(null);
		// }
		// catch(Exception e)
		// {
		// e.printStackTrace();
		// }

		try {

			this.connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		stoped = true;
	}

	public javax.jms.Message receiveNoWait(String destination) throws javax.jms.JMSException {
		ReceiveDispatcher dispatcher = null;
		try {
			dispatcher = new ReceiveDispatcher(this.connection, destination);
			Message msg = dispatcher.receiveNoWait();
			return msg;
		} finally {
			if (dispatcher != null) {
				dispatcher.stop();
			}
			// dispatcher.stop();
		}
	}

 
 
 

	public javax.jms.Message receive(String destination) throws javax.jms.JMSException {

		ReceiveDispatcher dispatcher = null;
		try {
			dispatcher = new ReceiveDispatcher(this.connection, destination);
			Message msg = dispatcher.receive();
			return msg;
		} finally {
			if (dispatcher != null) {
				dispatcher.stop();
			}
			// dispatcher.stop();
		}

	}

	public javax.jms.Message receive(String destination, long timeout) throws javax.jms.JMSException {
		ReceiveDispatcher dispatcher = null;
		try {
			dispatcher = new ReceiveDispatcher(this.connection, destination);
			Message msg = dispatcher.receive(timeout);
			return msg;
		} finally {
			if (dispatcher != null) {
				dispatcher.stop();
			}
		}
	}

	 


	public void setMessageListener(String destination, javax.jms.MessageListener listener)
			throws javax.jms.JMSException {
		ReceiveDispatcher dispatcher = null;
		try {
			dispatcher = new ReceiveDispatcher(this.connection, destination);
			if (listener instanceof JMSMessageListener) {
				JMSMessageListener temp = (JMSMessageListener) listener;
				temp.setReceivor(dispatcher);
			}
			dispatcher.setMessageListener(listener);

			tempdispatcher.add(dispatcher);
		} finally {
			dispatcher = null;
			// dispatcher.stop();
		}

	}

	public void receive(String destination, javax.jms.MessageListener listener) throws javax.jms.JMSException {
		setMessageListener(destination, listener);

	}

	public boolean isClientAcknowledge(RequestDispatcher requestDispatcher) throws JMSException {

		return requestDispatcher.isClientAcknowledge();
	}

	public void send(String destination, String message) throws JMSException {
		send(destination, message, false);
		// session.createProducer(arg0)
	}

	public void send(String destination, String message, boolean persistent) throws JMSException {
		send(MQUtil.TYPE_QUEUE, destination, message, persistent);

		// session.createProducer(arg0)
	}

	public void send(int desttype, String destination, String message) throws JMSException {
		send(desttype, destination, message, false);
		// session.createProducer(arg0)
	}

	public void send(int desttype, String destination, String message, boolean persistent) throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection, false, Session.AUTO_ACKNOWLEDGE, desttype, destination,
					null, persistent);
			dispatcher.send(message, (JMSProperties) null);
		} finally {
			if(dispatcher != null)
				dispatcher.stop();
		}
		// session.createProducer(arg0)
	}

	/**
	 * 单/批处理发送消息api
	 * @param destination
	 * @param callback
	 * @throws JMSException
	 */
	public void send(String destination,SendCallback callback) throws JMSException {
		send(MQUtil.TYPE_QUEUE,   destination,  callback);
	}
	/**
	 * 单/批处理发送消息api
	 * @param desttype
	 * @param destination
	 * @param callback
	 * @throws JMSException
	 */
	public void send(int desttype, String destination,SendCallback callback) throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection, callback.autocommit(), callback.ackMode(), desttype, destination,
					null, false);
			dispatcher.send(callback);
		} finally {
			if(dispatcher != null)
				dispatcher.stop();
		}
		// session.createProducer(arg0)
	}

	public void send(int desttype, String destination, String message, boolean persistent, JMSProperties properties)
			throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection, false, Session.AUTO_ACKNOWLEDGE, desttype, destination,
					null, persistent);
			dispatcher.send(message, properties);
		} finally {
			if(dispatcher != null)
				dispatcher.stop();
		}
		// session.createProducer(arg0)
	}

	// public void commitRequest() throws JMSException
	// {
	// if(this.requestDispatcher != null)
	// this.requestDispatcher.commit();
	// }

	// public void commitReply() throws JMSException
	// {
	// if(this.responseDispatcher != null)
	// this.responseDispatcher.commit();
	// }

	public void commit(RequestDispatcher requestDispatcher) throws JMSException {
		if (requestDispatcher != null)
			requestDispatcher.commit();
	}
	// public void rollbackRequest() throws JMSException
	// {
	// if(this.requestDispatcher != null)
	// this.requestDispatcher.rollback();
	// }
	// public void rollbackReply() throws JMSException
	// {
	// if(this.responseDispatcher != null)
	// this.responseDispatcher.rollback();
	// }

	public void rollback(RequestDispatcher requestDispatcher) throws JMSException {
		if (requestDispatcher != null)
			requestDispatcher.rollback();
	}

	

	
	

	// public void sendReply(Message msg,Logger logger) throws JMSException
	// {
	// this.responseDispatcher.send(msg,logger);
	// }

	// public void sendReply(Message msg,Logger logger) throws JMSException
	// {
	// this.responseDispatcher.send(msg,logger);
	// }

	public void send(int destinationType, String destination_, boolean persistent, int priority, long timeToLive,
			Message message, Logger step) throws JMSException {

		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, priority, timeToLive, message, step,
					(JMSProperties) null);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}

	}

	public void send(int destinationType, String destination_, boolean persistent, int priority, long timeToLive,
			Message message, Logger step, JMSProperties properties) throws JMSException {

		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, priority, timeToLive, message, step, properties);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}

	}

	public void send( String destination_, boolean persistent, int priority, long timeToLive,
			Message message) throws JMSException {

		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(MQUtil.TYPE_QUEUE, destination_, persistent, priority, timeToLive, message,
					(JMSProperties) null);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}
	public void send( String destination_, String message,boolean persistent, int priority, long timeToLive) throws JMSException {

		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(MQUtil.TYPE_QUEUE, destination_, persistent, priority, timeToLive, message,
					(JMSProperties) null);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}
	public void send(int destinationType, String destination_, boolean persistent, int priority, long timeToLive,
			Message message) throws JMSException {

		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, priority, timeToLive, message,
					(JMSProperties) null);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, boolean persistent, int priority, long timeToLive,
			Message message, JMSProperties properties) throws JMSException {

		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, priority, timeToLive, message, properties);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, boolean persistent, Message message, Logger logger)
			throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, message, logger, (JMSProperties) null);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, boolean persistent, Message message, Logger logger,
			JMSProperties properties) throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, message, logger, properties);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, boolean persistent, Message message)
			throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, message, (JMSProperties) null);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, boolean persistent, Message message,
			JMSProperties properties) throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, message, properties);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, boolean persistent, String message) throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, message, (JMSProperties) null);
		} finally {
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, boolean persistent, String message,
			JMSProperties properties) throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, message, properties);
		} finally {
//			dispatcher = null;
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, Message message, boolean persistent, int priority,
			long timeToLive) throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, message, persistent, priority, timeToLive,
					(JMSProperties) null);
		} finally {
//			dispatcher = null;
			if(dispatcher != null)
				 dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, Message message, boolean persistent, int priority,
			long timeToLive, JMSProperties properties) throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, message, persistent, priority, timeToLive, properties);
		} finally {
//			dispatcher = null;
			if(dispatcher != null)
				dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, String message, boolean persistent, int priority,
			long timeToLive) throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, priority, timeToLive, message,
					(JMSProperties) null);
		} finally {
			// dispatcher = null;
			if(dispatcher != null)
				dispatcher.stop();
		}
	}

	public void send(int destinationType, String destination_, String message, boolean persistent, int priority,
			long timeToLive, JMSProperties properties) throws JMSException {
		RequestDispatcher dispatcher = null;
		try {
			dispatcher = new RequestDispatcher(this.connection);
			dispatcher.send(destinationType, destination_, persistent, priority, timeToLive, message, properties);
		} finally {
			// dispatcher = null;
			if(dispatcher != null)
				dispatcher.stop();
		}
	}

	
}