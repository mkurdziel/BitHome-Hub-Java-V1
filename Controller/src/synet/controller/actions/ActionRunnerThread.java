package synet.controller.actions;

import synet.controller.NodeManager;
import synet.controller.actions.ActionRequest.EsnActionRequestStatus;
import synet.controller.messaging.MsgDispatcher;
import synet.controller.utils.Logger;

public class ActionRunnerThread implements Runnable{
	private static final String TAG = "ActionRunnerThread";
	private ActionRequest m_request;
	private NodeManager m_nodeManager;
	private ActionManager m_actionManager;
	private MsgDispatcher m_msgDispatcher;

	/**
	 * Default constructor 
	 * 
	 * @param request
	 */
	public ActionRunnerThread(
			ActionRequest p_request,
			NodeManager p_nodeManager,
			ActionManager p_actionManager,
			MsgDispatcher p_msgDispatcher)
	{
		m_request = p_request;
		m_nodeManager = p_nodeManager;
		m_actionManager = p_actionManager;
		m_msgDispatcher = p_msgDispatcher;
	}

	@Override
	public void run() {
		IAction action = m_request.getAction();
		
		action.prepareExecute();
		
		// Execution the action. We are in a thread here so we can do this
		if ( action.execute(
				m_nodeManager, 
				m_actionManager, 
				m_msgDispatcher,
				m_request.getTimeoutMilliseconds()))
		{
			m_request.setExecuted(EsnActionRequestStatus.EXECUTED);
		}
		else
		{
			Logger.i(TAG, "action failed to execute");
			m_request.setExecuted(EsnActionRequestStatus.ERROR);
			m_request.setErrorMessage(action.getExecuteErrorString());
		}
		
		action.finishExecute();
	}

}
