package synet.controller.messaging.messages;

import synet.controller.nodes.NodeBase;

public class MsgSystemCatalogTransmit extends MsgTx
{
	private static final String MSG_TYPE = "Catalog Transmit";
	
	private NodeBase m_node;
	
	/**
	 * Default constructor
	 * 
	 * @param p_destinationNode
	 * @param p_node
	 */
	public MsgSystemCatalogTransmit(NodeBase p_destinationNode, NodeBase p_node)
	{
		super(p_destinationNode);
		
		m_node = p_node;
	}

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
		return MsgConstants.SN_API_SYSTEM_CATALOG_TRANSMIT;
	}
	
	/**
	 * @return the node with a catalog change
	 */
	public NodeBase getNode()
	{
		return m_node;
	}
}
