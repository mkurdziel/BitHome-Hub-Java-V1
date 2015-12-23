/**
 * 
 */
package synet.controller.messaging.messages;

import synet.controller.messaging.messages.MsgConstants.EsnAPIBootloadTransmit;
import synet.controller.nodes.NodeBase;
import synet.controller.utils.Logger;

/**
 * @author mkurdziel
 *
 */
public class MsgBootloadTransmit extends MsgTx
{
    private static final String TAG = "MsgBootloadTransmit";
	private static final String MSG_TYPE = "Bootload Transmit";
	
	private EsnAPIBootloadTransmit m_state;
	private byte[] m_dataBytes;
	private int m_address;
	private int m_checksum;
	private int m_blockSize;
	
	public MsgBootloadTransmit(NodeBase p_destinationNode, EsnAPIBootloadTransmit p_state)
	{
		super(p_destinationNode);
		
		m_state = p_state;
	}

	/**
	 * @param p_destinationNode
	 * @param p_state
	 * @param p_dataBytes
	 * @param p_checksum 
	 */
	public MsgBootloadTransmit(
	        NodeBase p_destinationNode,
            EsnAPIBootloadTransmit p_state, 
            byte[] p_dataBytes, 
            int p_address,
            int p_checksum,
            int p_blockSize)
    {
	    this(p_destinationNode, p_state);
	    
	    m_dataBytes = p_dataBytes;
	    m_address = p_address;
	    m_checksum = p_checksum;
	    m_blockSize = p_blockSize;
    }
	
	/**
	 * @return the bootload type
	 */
	public EsnAPIBootloadTransmit getState()
	{
	    return m_state;
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
		return  MsgConstants.SN_API_BOOTLOAD_TRANSMIT;
	}

	@Override
	public byte[] getBytes()
	{
	    switch (m_state)
	    {
	        case REBOOT_DEVICE:
	        case BOOTLOAD_REQUEST:
	        case DATA_COMPLETE:
	        {
	            byte[] bytes = new byte[3];
				
				bytes[0] = (byte)MsgConstants.SYNET_START_BYTE;
				bytes[1] = (byte)MsgConstants.SN_API_BOOTLOAD_TRANSMIT;
				bytes[2] = (byte)m_state.getCode();
				
				return bytes;
	        }
	        case DATA_TRANSMIT:
	        {
	            byte[] bytes = new byte[7+m_dataBytes.length];
	            bytes[0] = (byte)MsgConstants.SYNET_START_BYTE;
	            bytes[1] = (byte)MsgConstants.SN_API_BOOTLOAD_TRANSMIT;
	            bytes[2] = (byte)m_state.getCode();
	            bytes[3] = (byte)m_blockSize;
	            bytes[4] = (byte)(m_address>>8);
	            bytes[5] = (byte)(m_address);
	            bytes[6] = (byte)m_checksum;
	            
	            int length = m_dataBytes.length;
	            for (int i=0; i<length; ++i)
	            {
	               bytes[i+7] = m_dataBytes[i]; 
	            }
	            
	            return bytes;
	        }
	            
	    }
	    
		return null;
	}
}
