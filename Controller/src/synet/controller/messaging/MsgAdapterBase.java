package synet.controller.messaging;

import synet.controller.configuration.Configuration;
import synet.controller.messaging.messages.Msg;
import synet.controller.messaging.messages.MsgSystemNodelistReceive;
import synet.controller.messaging.messages.MsgTx;

public abstract class MsgAdapterBase {
	private MsgReceivedEventListener m_listener = null;
	private boolean m_isStarted = false;
	private Configuration m_config;
	
	/**
	 * Start the message adapter
	 * 
	 * @return true if the message adapter was started successfully
	 */
	public abstract boolean startAdapater();
	
	/**
	 * Handle any base class adapter setup
	 */
	protected void startBase()
	{
		m_isStarted = true;
	}
	
	/**
	 * Stop the adapter
	 */
	public abstract void stopAdapter();
	
	/**
	 * Handle any base class adapter teardown
	 */
	protected void stopBase()
	{
		m_isStarted = false;
	}
	
	/**
	 * @return true if adapter is already started
	 */
	public boolean getIsStarted()
	{
		return m_isStarted;
	}
	
	/**
	 * Set the configuration object for the adapter
	 * 
	 * @param p_config
	 */
	public final void setConfiguration(Configuration p_config)
	{
		m_config = p_config;
	}
	
	/**
	 * @return the configuration for the adapter
	 */
	public Configuration getConfiguration()
	{
		return m_config;
	}
	
	/**
	 * Set the msg received listener for this adapter
	 * @param p_listener
	 */
	public final synchronized void setMsgReceivedListener(MsgReceivedEventListener p_listener)
	{
		m_listener = p_listener;
	}
	
	/**
	 * Fire the message received event
	 */
	protected synchronized void fireMsgReceivedEvent(Msg p_msg)
	{
		if (m_listener != null)
		{
			MsgReceivedEvent event = new MsgReceivedEvent(this, p_msg);
			m_listener.handleMsgReceivedEvent(event);
		}
	}
	
	/**
	 * Process a system node list receive message and create the appropriate node
	 * 
	 * @param p_msg
	 */
	public abstract void processSystemNodeListReceive(MsgSystemNodelistReceive p_msg);
	
	/**
	 * @return the node class that this adapter handles
	 */
	public abstract Class<?> getNodeClass();
	
	/**
	 * @return an identifying String for this node class type
	 */
	public abstract String getNodeTypeIdentifierString();
	
	public abstract void sendMessage(MsgTx p_msg);

	public abstract void broadcastMessage(MsgTx pMsg);
}
