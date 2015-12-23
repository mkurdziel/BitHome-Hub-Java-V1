/**
 * 
 */
package synet.controller.messaging;

import synet.controller.messaging.messages.Msg;
import synet.controller.messaging.messages.MsgBootloadResponse;
import synet.controller.messaging.messages.MsgCatalogResponse;
import synet.controller.messaging.messages.MsgConstants;
import synet.controller.messaging.messages.MsgDeviceStatusResponse;
import synet.controller.messaging.messages.MsgFunctionReceive;
import synet.controller.messaging.messages.MsgParameterResponse;
import synet.controller.nodes.NodeBase;
import synet.controller.utils.Logger;

/**
 * Base message factory for creating synet messages
 * 
 * @author mkurdziel
 *
 */
public class MsgFactory
{
	private static final String TAG = "MsgFactory";

	public static Msg createMessage(NodeBase p_sourceNode,
			NodeBase p_destinationNode,
			int[] p_data, 
			int p_startIndex)
	{
			
		// Make sure the first byte is our start byte
		if (p_data[p_startIndex] != MsgConstants.SYNET_START_BYTE)
		{
			Logger.w(TAG, "received message without start byte. Read:" + String.format("0x%x", p_data[p_startIndex]));
			return null;
		}

		// Switch on the API byte
		switch(p_data[p_startIndex+MsgConstants.SYNET_INDEX_API])
		{
		case MsgConstants.SN_API_DEVICE_STATUS_REQUEST:
			Logger.v(TAG, "Device Status Request");
			break;
		case MsgConstants.SN_API_DEVICE_STATUS_RESPONSE:
			return new MsgDeviceStatusResponse(p_sourceNode, p_destinationNode, p_data, p_startIndex);
		case MsgConstants.SN_API_BOOTLOAD_TRANSMIT:
			Logger.v(TAG, "Device Bootload Transmit");
			break;
		case MsgConstants.SN_API_BOOTLOAD_RESPONSE:
		    return new MsgBootloadResponse(p_sourceNode, p_destinationNode, p_data, p_startIndex);
		case MsgConstants.SN_API_SET_INFO:
			Logger.v(TAG, "Device Set Info");
			break;
		case MsgConstants.SN_API_SET_INFO_RESPONSE:
			Logger.v(TAG, "Device Set Info Response");
			break;
		case MsgConstants.SN_API_CATALOG_REQUEST:
			Logger.v(TAG, "Device Catalog Request");
			break;
		case MsgConstants.SN_API_CATALOG_RESPONSE:
			return new MsgCatalogResponse(p_sourceNode, p_destinationNode, p_data, p_startIndex);
		case MsgConstants.SN_API_PARAMETER_REQUEST:
			Logger.v(TAG, "Device Parameter Request");
			break;
		case MsgConstants.SN_API_PARAMETER_RESPONSE:
			Logger.v(TAG, "Device Parameter Response");
			return new MsgParameterResponse(p_sourceNode, p_destinationNode, p_data, p_startIndex);
		case MsgConstants.SN_API_FUNCTION_TRANSMIT:
			Logger.v(TAG, "Device Function Transmit");
			break;
		case MsgConstants.SN_API_FUNCTION_TRANSMIT_RESPONSE:
			Logger.v(TAG, "Device Transmit Response");
			break;
		case MsgConstants.SN_API_FUNCTION_RECEIVE:
		    return new MsgFunctionReceive(p_sourceNode, p_destinationNode, p_data, p_startIndex);
		case MsgConstants.SN_API_FUNCTION_RECEIVE_RESPONSE:
			Logger.v(TAG, "Device Function Receive Response");
			break;
		default:
			Logger.w(TAG, "received a message with an invalid API:" + String.format("0x%x", p_data[p_startIndex+MsgConstants.SYNET_INDEX_API]));
		}
		return null;
	}
}
