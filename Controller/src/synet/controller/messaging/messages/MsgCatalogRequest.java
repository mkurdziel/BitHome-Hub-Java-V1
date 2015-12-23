/**
 * 
 */
package synet.controller.messaging.messages;

import synet.controller.nodes.NodeBase;

/**
 * @author mkurdziel
 *
 */
public class MsgCatalogRequest extends MsgTx
{
	public static final int FULL_CATALOG_REQUEST = 0x00;
	
	private static final String TAG = "MsgCatalogRequest";
	private int m_functionNum;
	
	/**
	 * Default constructor
	 * Request a function number
	 * 0x00 = Full Catalog Request
	 * 0x01 = Request function ID 1
	 * 0x02 = Request function ID 2
	 * etc...
	 * 
	 * @param p_destinationNode
	 * @param p_functionNum
	 */
	public MsgCatalogRequest(NodeBase p_destinationNode, int p_functionNum)
	{
		super(p_destinationNode);
		
		m_functionNum = p_functionNum;
	}

	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getMsgType()
	 */
	@Override
	public String getMsgType()
	{
		return TAG;
	}

	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getAPI()
	 */
	@Override
	public byte getAPI()
	{
		return MsgConstants.SN_API_CATALOG_REQUEST;
	}

	@Override
	public byte[] getBytes()
	{
		byte[] bytes = new byte[3];
		
		bytes[0] = (byte)MsgConstants.SYNET_START_BYTE;
		bytes[1] = (byte)MsgConstants.SN_API_CATALOG_REQUEST;
		bytes[2] = (byte)m_functionNum;
		
		return bytes;
	}
	
	/**
	 * @return the action index requested
	 */
	public int getActionIndex()
	{
	    return m_functionNum;
	}
	
	@Override
	public String getDescription()
	{
	    return String.format("%s actionIndex: %d", TAG, m_functionNum);
	}

}
