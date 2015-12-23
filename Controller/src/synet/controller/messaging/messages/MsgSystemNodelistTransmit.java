package synet.controller.messaging.messages;

import synet.controller.nodes.NodeBase;

/**
 * @author mkurdziel
 *
 */
public class MsgSystemNodelistTransmit extends MsgTx
{
	private static final String MSG_TYPE = "Node List Transmit";
	
	private NodeBase[] m_nodeList;

	/**
	 * @param p_destinationNode
	 * @param p_nodeList
	 */
	public MsgSystemNodelistTransmit(NodeBase p_destinationNode, NodeBase[] p_nodeList)
	{
		super(p_destinationNode);
		
		m_nodeList = p_nodeList;
	}

	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.MsgTx#getBytes()
	 */
	@Override
	public byte[] getBytes()
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getMsgType()
	 */
	@Override
	public String getMsgType()
	{
		return MSG_TYPE;
	}

	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getAPI()
	 */
	@Override
	public byte getAPI()
	{
		return MsgConstants.SN_API_SYSTEM_NODELIST_TRANSMIT;
	}
	
	/**
	 * @return the node list
	 */
	public NodeBase[] getNodeList()
	{
		return m_nodeList;
	}
	
	

}
