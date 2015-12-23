/**
 * 
 */
package synet.controller.messaging.messages;

import synet.controller.messaging.messages.MsgConstants.EsnAPIBootloadResponse;
import synet.controller.nodes.NodeBase;
import synet.controller.utils.EBitConverter;
import synet.controller.utils.Logger;

/**
 * @author mkurdziel
 *
 */
public class MsgBootloadResponse extends Msg
{
	private static final String MSG_TYPE = "Bootload Response";
	private static final String TAG = "MsgBootloadResponse";
	
	private int m_memoryAddress = 0;
	private MsgConstants.EsnAPIBootloadResponse m_response;

	/**
	 * Data constructor
	 * 
	 * @param p_data
	 * @param p_dataOffset
	 */
	public MsgBootloadResponse(
	        NodeBase p_sourceNode,
            NodeBase p_destinationNode,
            int[] p_data, int p_dataOffset)
	{
	    super(p_sourceNode, p_destinationNode);
	   
		m_response = EsnAPIBootloadResponse.get(p_data[p_dataOffset+2]);
		
		// Memory address if only valid for data success command
		if (m_response == EsnAPIBootloadResponse.DATA_SUCCESS)
		{
			m_memoryAddress = EBitConverter.toUInt16(p_data, p_dataOffset + 3);
		}
		
		Logger.v(TAG,"Status:" + m_response +  " Mem Address:"+m_memoryAddress);
	}
	
	/**
	 * @return the bootload response
	 */
	public EsnAPIBootloadResponse getBootloadResponse()
	{
		return m_response;
	}
	
	/**
	 * @return the memory address
	 */
	public int getMemoryAddress()
	{
		return m_memoryAddress;
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
        return MsgConstants.SN_API_BOOTLOAD_RESPONSE;
    }

}
