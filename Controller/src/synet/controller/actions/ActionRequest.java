package synet.controller.actions;

import synet.controller.utils.Logger;

/**
 * Class to represent an Action execution request.
 */
public class ActionRequest {
	public final static String TAG = "ActionRequest";
	public enum EsnActionRequestStatus
	{
		UNKNOWN,
		// An error occurred during the execution
		ERROR,
		// Action has been requested
		REQUESTED,
		// Action has been successfully executed
		EXECUTED,
	}

	private EsnActionRequestStatus m_status = EsnActionRequestStatus.UNKNOWN;

	private final Object m_executionWaitObject = new Object();
	private boolean m_isExecuted = false;
	private String m_errorString = "";
	private long m_timeoutMilliseconds;

	private IAction m_action;

	protected ActionRequest(IAction p_action, long p_timeoutMilliseconds) 
	{
		if (p_action == null)
		{
			setExecuted(EsnActionRequestStatus.ERROR);
			m_isExecuted = true;
		}
		m_timeoutMilliseconds = p_timeoutMilliseconds;
		m_action = p_action;
	}

	/**
	 * @return the action for this request
	 */
	public IAction getAction()
	{
		return m_action;
	}
	
	/**
	 * @param p_msg
	 */
	public void setErrorMessage(String p_msg)
	{
		m_errorString = p_msg;
	}
	
	/**
	 * @return an optional error string
	 */
	public String getErrorMessage()
	{
		return m_errorString;
	}

	/**
	 * Set the execution status whether successful or otherwise
	 * 
	 * @param p_exeStatus
	 */
	protected void setExecuted(EsnActionRequestStatus p_status)
	{
		synchronized(m_executionWaitObject)
		{
			Logger.v(TAG, "Setting execution status to " + p_status);
			m_status = p_status;
			m_isExecuted = true;
			m_executionWaitObject.notifyAll();
		}
	}

	/**
	 * @return the status of the request
	 */
	public EsnActionRequestStatus getStatus()
	{
		return m_status;
	}
	
	/**
	 * @return the timeout milliseconds
	 */
	public long getTimeoutMilliseconds()
	{
		return m_timeoutMilliseconds;
	}

	/**
	 * Wait for the execution to time out
	 * 
	 * @return
	 */
	public boolean waitForExecution()
	{
		if (!m_isExecuted)
		{
			synchronized(m_executionWaitObject)
			{
				try {
					m_executionWaitObject.wait(m_timeoutMilliseconds);
				} catch (InterruptedException e) {
					System.err.println("ActionRequest interrupted");
					return false;
				}
			}
		}
		return m_status == EsnActionRequestStatus.EXECUTED;
	}
}
