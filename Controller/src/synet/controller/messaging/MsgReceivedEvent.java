/**
 * 
 */
package synet.controller.messaging;

import java.util.EventObject;

import synet.controller.messaging.messages.Msg;

/**
 * Event class for messages being received by the adapters
 */
public class MsgReceivedEvent extends EventObject {

	private static final long serialVersionUID = -5928314240060280166L;
	
	private Msg m_msg;

	/**
	 * Default constructor
	 * 
	 * @param p_source
	 * @param p_msg
	 */
	public MsgReceivedEvent(Object p_source, Msg p_msg) {
		super(p_source);
		m_msg = p_msg;
	}
	
	/**
	 * @return the message for the event
	 */
	public Msg getMessage()
	{
		return m_msg;
	}
}
