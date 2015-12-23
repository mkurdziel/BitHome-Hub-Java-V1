/**
 * Base class for all SyNet messages
 */
package synet.controller.messaging.messages;

import java.util.Date;

import synet.controller.nodes.NodeBase;

/**
 * @author kur57360
 *
 */
public abstract class Msg {
	
	private Date m_timestamp;
	private NodeBase m_sourceNode;
	private NodeBase m_destNode;
	private boolean m_isSynchronous = false;
	
	
	/**
	 * Timestamp constructor
	 * 
	 * @param p_timestamp
	 */
	public Msg(Date p_timestamp) 
	{
		m_timestamp = p_timestamp;
	}
	
	/**
	 * Default constructor
	 * @param p_destinationNode 
	 * @param p_sourceNode 
	 */
	public Msg(NodeBase p_sourceNode, NodeBase p_destinationNode)
	{
		// Initialize the message with a timestamp
		this(new Date());
		
		m_sourceNode = p_sourceNode;
		m_destNode = p_destinationNode;
	}
	
	/**
	 * Default constructor
	 */
	public Msg()
	{
		this(new Date());
	}
	
	/**
	 * Set if this message is synchronous
	 */
	public void setIsSynchronous(boolean p_isS)
	{
		m_isSynchronous = p_isS;
	}
	
	/**
	 * @return true if this message is synchronous
	 */
	public boolean getIsSynchronous()
	{
		return m_isSynchronous;
	}
	
	/**
	 * @return the timestamp that the message was created
	 */
	public Date getTimestamp()
	{
		return m_timestamp;
	}
	
	/**
	 * @return a string description of the message
	 */
	public String getDescription()
	{
		return getMsgType();
	}
	
	/**
	 * @return the message type
	 */
	public abstract String getMsgType();
	
	/**
	 * @return the API value for this message
	 */
	public abstract byte getAPI();
	
	/**
	 * Set the source node of the message
	 * 
	 * @param p_node
	 */
	public void setSourceNode(NodeBase p_node)
	{
		m_sourceNode = p_node;
	}
	
	/**
	 * @return the source node of the message
	 */
	public NodeBase getSourceNode()
	{
		return m_sourceNode;
	}
	
	/**
	 * Set the destination node
	 * @param p_node
	 */
	public void setDestinationNode(NodeBase p_node)
	{
		m_destNode = p_node;
	}
	
	/**
	 * @return the destination node of the message
	 */
	public NodeBase getDestinationNode()
	{
		return m_destNode;
	}
}
