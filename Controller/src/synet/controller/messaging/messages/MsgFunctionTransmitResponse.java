/**
 * 
 */
package synet.controller.messaging.messages;

/**
 * @author mkurdziel
 *
 */
public class MsgFunctionTransmitResponse extends Msg
{
	private static final String TAG = "MsgFunctionTransmitResponse";

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
		return MsgConstants.SN_API_FUNCTION_TRANSMIT_RESPONSE;
	}
}
