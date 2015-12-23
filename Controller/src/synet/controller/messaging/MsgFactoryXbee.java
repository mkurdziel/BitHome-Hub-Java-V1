package synet.controller.messaging;

import synet.controller.NodeManager;
import synet.controller.messaging.messages.Msg;
import synet.controller.messaging.messages.MsgXbeeTxStatus;
import synet.controller.nodes.NodeBase;
import synet.controller.nodes.NodeZigbee;
import synet.controller.utils.EBitConverter;
import synet.controller.utils.Logger;

public class MsgFactoryXbee 
{
	private static final String TAG = "MsgFactoryXbee";

	public static final int C_XBEE_API_START_BYTE = 0x7E;

	public static final int C_XBEE_API_INVALID = 0x00;
	public static final int C_XBEE_API_MODEM_STATUS = 0xBA;
	public static final int C_XBEE_API_AT_CMD = 0x08;
	public static final int C_XBEE_API_AT_CMD_PARAM = 0x09;
	public static final int C_XBEE_API_AT_CMD_RESPONSE = 0x88;
	public static final int C_XBEE_API_REMOTE_CMD = 0x17;
	public static final int C_XBEE_API_CMD_RESPONSE = 0x97;
	public static final int C_XBEE_API_ZIGBEE_TX_REQ = 0x10;
	public static final int C_XBEE_API_ZIGBEE_CMD_FRAME = 0x11;
	public static final int C_XBEE_API_ZIGBEE_TX_STATUS = 0x8B;
	public static final int C_XBEE_API_ZIGBEE_RX = 0x90;
	public static final int C_XBEE_API_ZIGBEE_IO = 0x92;
	public static final int C_XBEE_API_RX_SENSOR = 0x94;
	public static final int C_XBEE_API_NODE_IDENT = 0x95;

	private static final int C_XBEE_RX_DATA_OFFSET = 11;
	private static final int C_XBEE_RX_ADDR64_OFFSET = 0;
	private static final int C_XBEE_RX_ADDR16_OFFSET = 8;

	private static final int C_XBEE_TXS_FRAME_OFFSET = 0;
	@SuppressWarnings("unused")
    private static final int C_XBEE_TXS_ADDR16_OFFSET = 1;
	private static final int C_XBEE_TXS_RETRY_OFFSET = 3;
	private static final int C_XBEE_TXS_STATUS_OFFSET = 4;
	private static final int C_XBEE_TXS_DISCOVERY_OFFSET = 5;

	private NodeManager m_nodeManager;

	/**
	 * Default constructor
	 */
	public MsgFactoryXbee()
	{
		m_nodeManager = NodeManager.getInstance();
	}

	/**
	 * Create an Xbee message
	 * 
	 * @param p_api
	 * @param p_data
	 * @return
	 */
	public Msg createMessage(int p_api, int[] p_data)
	{
		switch(p_api)
		{
		case C_XBEE_API_ZIGBEE_RX:
		{
			// Extract the 64 bit address
			long address64 = EBitConverter.toUInt64(p_data, C_XBEE_RX_ADDR64_OFFSET);
			// Extract the 16 bit address
			int address16 = EBitConverter.toUInt16(p_data, C_XBEE_RX_ADDR16_OFFSET);

			Logger.v(TAG, "received Zigbee RX message 64:"+ String.format("0x%x", address64)
					+ " 16:" + String.format("0x%x", address16));

			// Look up the node
			NodeBase node = m_nodeManager.getNode(address64);

			if (node == null)
			{
				node = new NodeZigbee(address64, address16);
				m_nodeManager.addNewNode(address64, node);
			}

			return MsgFactory.createMessage(node, null, p_data, C_XBEE_RX_DATA_OFFSET);
		}

		case C_XBEE_API_ZIGBEE_TX_STATUS:
		{
//			Logger.v(TAG, "TXStatus - Frame:"+p_data[C_XBEE_TXS_FRAME_OFFSET]+" Addr16:"+String.format("0x%x", EBitConverter.toUInt16(p_data, C_XBEE_TXS_ADDR16_OFFSET)) +
//					" retry:" + p_data[C_XBEE_TXS_RETRY_OFFSET] +
//					" status:" + String.format("0x%x", p_data[C_XBEE_TXS_STATUS_OFFSET])+
//					" discovery:" + String.format("0x%x", p_data[C_XBEE_TXS_DISCOVERY_OFFSET]));
			return new MsgXbeeTxStatus(
					p_data[C_XBEE_TXS_FRAME_OFFSET], 
					p_data[C_XBEE_TXS_RETRY_OFFSET], 
					p_data[C_XBEE_TXS_STATUS_OFFSET], 
					p_data[C_XBEE_TXS_DISCOVERY_OFFSET]);
		}
		default:
		{
			Logger.d(TAG, "received unknown message api:" + String.format("0x%x", p_api));
		}

		}
		return null;
	}

}
