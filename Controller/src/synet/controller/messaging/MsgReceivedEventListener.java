package synet.controller.messaging;

/**
 * Event listener interface for messages being received
 */
public interface MsgReceivedEventListener {
	public void handleMsgReceivedEvent(MsgReceivedEvent p_eventObject);
}
