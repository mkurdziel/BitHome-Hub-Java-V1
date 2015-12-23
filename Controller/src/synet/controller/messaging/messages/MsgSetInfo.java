/**
 * 
 */
package synet.controller.messaging.messages;

/**
 * @author mkurdziel
 *
 */
public class MsgSetInfo extends Msg
{
	private static final String MSG_TYPE = "Set Info";

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
		return MsgConstants.SN_API_SET_INFO;
	}
}
