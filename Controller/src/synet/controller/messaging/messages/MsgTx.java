/**
 * 
 */
package synet.controller.messaging.messages;

import synet.controller.nodes.NodeBase;

/**
 * @author mkurdziel
 *
 */
/**
 * @author kur57360
 *
 */
public abstract class MsgTx extends Msg
{
	private Object m_sendLockObject = new Object();
	private boolean m_isSent = false;
	private String m_errorMsg;
	private boolean m_isError = false;
	
	/**
	 * Default constructor
	 * 
	 * @param p_destinationNode
	 */
	public MsgTx(NodeBase p_destinationNode)
	{
		super(null, p_destinationNode);
	}
	
	/**
	 * @return bytes to be sent
	 */
	public abstract byte[] getBytes();
	
	/**
	 * Set the sent status of this message to true
	 */
	public void setIsSent()
	{
		synchronized (m_sendLockObject) {
			m_isSent = true;
			m_sendLockObject.notifyAll();
		}
	}
	
	/**
	 * Wait for the message to send
	 * @param p_milliseconds
	 * @return
	 */
	public boolean waitForSend(long p_milliseconds)
	{
		// If already sent, return true as a success
		if (m_isSent) return true;
		
		// If not yet sent and no error, wait
		else if (!m_isError)
		{
			synchronized (m_sendLockObject)
			{
				try {
					m_sendLockObject.wait(p_milliseconds);
				} catch (InterruptedException e) {
					// We didn't get a send notification within the 
					// requested timeout
					return false;
				}
			}
			
			return m_isSent;
		}
		return false;
	}
	
	/**
	 * Set the error message and error flag
	 * 
	 * @param p_errorMsg
	 */
	public void setErrorMsg(String p_errorMsg)
	{
		m_isError = true;
		m_errorMsg = p_errorMsg;
	}
	
	/**
	 * @return the error message string
	 */
	public String getErrorMsg()
	{
		return m_errorMsg;
	}
	
	/**
	 * @return true if this message was sent successfully
	 */
	public boolean getIsSent()
	{
		return m_isSent;
	}
	
	/**
	 * @return true if there was an error sending
	 */
	public boolean getIsError()
	{
		return m_isError;
	}
	
}
