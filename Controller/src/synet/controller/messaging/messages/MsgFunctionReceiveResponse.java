/**
 * 
 */
package synet.controller.messaging.messages;

/**
 * @author mkurdziel
 *
 */
public class MsgFunctionReceiveResponse extends Msg
{
	private static final String TAG = "MsgFunctionReceiveResponse";

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
		return MsgConstants.SN_API_FUNCTION_RECEIVE_RESPONSE;
	}
	
	
}
