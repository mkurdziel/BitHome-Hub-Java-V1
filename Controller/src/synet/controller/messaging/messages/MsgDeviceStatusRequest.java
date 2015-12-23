/**
 * 
 */
package synet.controller.messaging.messages;

import synet.controller.nodes.NodeBase;

/**
 * @author mkurdziel
 *
 */
public class MsgDeviceStatusRequest extends MsgTx
{
	/**
	 * Enumeration for the request type
	 */
	public enum EsnRequestType
	{
		STATUS,
		INFORMATION
	}
	
	private static final String MSG_TYPE = "Device Status Request";
	private static final int MSG_SIZE = 3;
	
	private EsnRequestType m_type;

	/**
	 * Default constructor
	 * 
	 * @param p_destinationNode
	 */
	public MsgDeviceStatusRequest(NodeBase p_destinationNode, EsnRequestType p_type)
	{
		super(p_destinationNode);
		
		m_type = p_type;
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
	 * @see synet.controller.messaging.messages.MsgTx#getBytes()
	 */
	@Override
	public byte[] getBytes()
	{
		byte[] bytes = new byte[MSG_SIZE];
		bytes[0] = (byte)MsgConstants.SYNET_START_BYTE;
		bytes[1] = MsgConstants.SN_API_DEVICE_STATUS_REQUEST;
		bytes[2] = (byte)m_type.ordinal();
		
		return bytes;
	}

	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getAPI()
	 */
	@Override
	public byte getAPI()
	{
		return MsgConstants.SN_API_DEVICE_STATUS_REQUEST;
	}
	
	/**
	 * @return the request type
	 */
	public EsnRequestType getRequestType()
	{
	    return m_type;
	}
}
