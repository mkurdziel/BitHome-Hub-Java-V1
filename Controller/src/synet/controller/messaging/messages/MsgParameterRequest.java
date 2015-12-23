/**
 * 
 */
package synet.controller.messaging.messages;

import synet.controller.nodes.NodeBase;

/**
 * @author mkurdziel
 *
 */
public class MsgParameterRequest extends MsgTx
{
	private static final String TAG = "MsgParameterRequest";
	
	private int m_functionNum;
	private int m_paramNum;

	/**
	 * Default constructor 
	 * Request a parameter
	 * 
	 * @param p_destinationNode
	 * @param p_functionNum
	 * @param p_parameterNum
	 */
	public MsgParameterRequest(NodeBase p_destinationNode, 
			int p_functionNum,
			int p_parameterNum)
	{
		super(p_destinationNode);
		
		m_functionNum = p_functionNum;
		m_paramNum = p_parameterNum;
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
		return MsgConstants.SN_API_PARAMETER_REQUEST;
	}

	@Override
	public byte[] getBytes()
	{
		byte[] bytes = new byte[4];
		
		bytes[0] = (byte)MsgConstants.SYNET_START_BYTE;
		bytes[1] = (byte)MsgConstants.SN_API_PARAMETER_REQUEST;
		bytes[2] = (byte)m_functionNum;
		bytes[3] = (byte)m_paramNum;
		
		return bytes;
	}
	
	/**
	 * @return the action index
	 */
	public int getActionIndex()
	{
	    return m_functionNum;
	}
	
	/**
	 * @return the parameter index
	 */
	public int getParameterIndex()
	{
	    return m_paramNum;
	}
	
	@Override
	public String getDescription()
	{
	    return String.format("%s actionIndex:%d paramIndex:%d", TAG, m_functionNum, m_paramNum);
	}
}
