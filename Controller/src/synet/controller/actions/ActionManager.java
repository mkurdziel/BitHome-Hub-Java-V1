package synet.controller.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import synet.controller.NodeManager;
import synet.controller.Protocol.EsnDataTypes;
import synet.controller.Protocol.EsnParamValidationTypes;
import synet.controller.messaging.MsgDispatcher;
import synet.controller.messaging.messages.MsgSystemNodelistTransmit;
import synet.controller.messaging.messages.MsgSystemUserActionListReceive;
import synet.controller.messaging.messages.MsgSystemUserActionListTransmit;
import synet.controller.nodes.NodeBase;
import synet.controller.nodes.NodeBroadcast;
import synet.controller.utils.Logger;

public class ActionManager {
	private static final int C_I_ACTION_TIMEOUT = 5000;
	private static final String TAG = "ActionManager";
	private static int MAX_ACTION_THREADS = 10;
	private static int MAX_POOL_SIZE = 20;
	private static int CORE_POOL_SIZE = 10;
	private static long KEEP_ALIVE_TIME = 60;
	private static int MAX_SHORT = 65535;

	private static ActionManager m_instance;

	// Random generator for action IDs
	private Random m_randGenerator = new Random();

	private ActionMap m_actionMap;

	private NodeManager m_nodeManager;
	private MsgDispatcher m_msgDispatcher;
	
	private NodeBroadcast m_broadcastNode = new NodeBroadcast();

	// Threading for running the Actions
	BlockingQueue<Runnable> m_threadPool = new ArrayBlockingQueue<Runnable>(MAX_ACTION_THREADS);
	RejectedExecutionHandler m_rejectedExectionHandler = new RejectedActionExecutionHandler();
	ThreadPoolExecutor m_actionExecutor = new ThreadPoolExecutor(
			CORE_POOL_SIZE, 
			MAX_POOL_SIZE, 
			KEEP_ALIVE_TIME, 
			TimeUnit.SECONDS, 
			m_threadPool,
			m_rejectedExectionHandler);


	////////////////////////////////////////////////////////////	
	// Event Listeners
	////////////////////////////////////////////////////////////	
	//	private EventListener<NodeFunction> m_onNodeFunctionAdded = new EventListener<NodeFunction>()
	//	{
	//		@Override
	//		public void onNotify(Object source, NodeFunction p_nodeFunction)
	//		{
	//			if (p_nodeFunction != null)
	//			{
	////				addNodeAction(p_nodeFunction);
	//			}
	//			else
	//			{
	//				Logger.e(TAG, "received notification with null node function");
	//			}
	//		}
	//	};


	/**
	 * @return the NodeManager instance
	 */
	public static synchronized ActionManager getInstance()
	{
		if (m_instance == null)
		{
			m_instance = new ActionManager();
		}
		return m_instance;
	}

	/**
	 * Default constructor
	 */
	private ActionManager()
	{
		Logger.d(TAG, "ActionManager created");
		m_actionMap = new ActionMap();
	}

	/**
	 * Start the Action Manager
	 */
	public void start()
	{
		Logger.i(TAG, "Starting");

		if (m_nodeManager != null)
		{
			//			m_nodeManager.onNodeFunctionAdded(m_onNodeFunctionAdded);
		}
		else
		{
			Logger.e(TAG, "cannot start with null NodeManager");
		}
	}

	/**
	 * Stop the Action Manager
	 */
	public void stop()
	{
		m_actionExecutor.shutdown();
		
		m_instance = null;
		
		Logger.i(TAG, "Stopped");
	}

	/**
	 * Set the node manager to be used by the action manager
	 * 
	 * @param p_nodeManager
	 */
	public void setNodeManager(NodeManager p_nodeManager)
	{
		m_nodeManager = p_nodeManager;
	}

	/**
	 * Set the Message Dispatcher used by the Ac
	 * @param p_msgDispatcher
	 */
	public void setMsgDispatcher(MsgDispatcher p_msgDispatcher)
	{
		m_msgDispatcher = p_msgDispatcher;
	}

	/**
	 * @return a unique Action ID that is not currently in the list
	 */
	private short getUniqueActionId()
	{
		short uniqueId = (short)m_randGenerator.nextInt(MAX_SHORT);

		// We need to synchronize this so that we know it's truly unique
		synchronized(m_actionMap)
		{
			while(m_actionMap.getAction(uniqueId) != null)
			{
				uniqueId = (short)m_randGenerator.nextInt(MAX_SHORT);
			}
		}
		return uniqueId;
	}

	/**
	 * @return a unique Parameter ID that is not currently in the list
	 */
	private short getUniqueParameterId()
	{
		short uniqueId = (short)m_randGenerator.nextInt(MAX_SHORT);

		// We need to synchronize this so that we know it's truly unique
		synchronized(m_actionMap)
		{
			while(m_actionMap.getParameter(uniqueId) != null)
			{
				uniqueId = (short)m_randGenerator.nextInt(MAX_SHORT);
			}
		}
		return uniqueId;
	}


	/**
	 * Get the Node Actions for a particular node ID
	 * 
	 * @param p_nodeId
	 * @return
	 */
	public ArrayList<INodeAction> getNodeActions(long p_nodeId)
	{
		return m_actionMap.getNodeActions(p_nodeId);
	}

	/**
	 * @param p_nodeId
	 * @param p_xml
	 * @return
	 */
	public void addNodeAction(INodeAction p_newNodeAction)
	{
		NodeBase node = m_nodeManager.getNode(p_newNodeAction.getNodeId()); 

		if (node != null)
		{
			INodeAction replacedAction = node.setNodeAction(p_newNodeAction.getFunctionId(), p_newNodeAction);
			if (replacedAction != null)
			{
			    Logger.w(TAG, "addNodeAction - replacing action " + p_newNodeAction.getDescription());
			    m_actionMap.removeNodeAction(p_newNodeAction.getNodeId(), replacedAction);
			}
			m_actionMap.addNodeAction(p_newNodeAction);
		}
		else
		{
			Logger.w(TAG, "adding node action to null node " + String.format("0x%x", p_newNodeAction.getNodeId()));
		}
	}


	/**
	 * @param nodeId
	 * @param entryNumber
	 * @param functionName
	 * @param returnType
	 * @param numParams
	 * @param paramType
	 * @return
	 */
	public INodeAction addNodeAction(
			NodeBase p_node, 
			int p_entryNumber,
			String p_name, 
			EsnDataTypes p_returnType, 
			int p_numParams,
			HashMap<Integer, EsnDataTypes> p_paramTypes)
	{
		Logger.i(TAG, String.format("adding node action %s for node %X", p_name, p_node.getNodeId()));

		NodeAction action = new NodeAction(
				getUniqueActionId(),
				p_node.getNodeId(),
				p_entryNumber,
				p_name,
				p_returnType,
				p_numParams,
				p_paramTypes);

		// Keep track of it in our map
		m_actionMap.addNodeAction(action);

		// Add the node action to the node
		p_node.setNodeAction(p_entryNumber, action);

		return action;
	}

	/**
	 * Add an already created Node parameter
	 * 
	 * @param p_newParameter
	 */
	public void addNodeParameter(NodeParameter p_newParameter)
	{
		NodeAction action = (NodeAction)m_actionMap.getAction(p_newParameter.getActionId());

		if (action != null)
		{
			// Add the parameter to the action
			action.addParameter(p_newParameter);

			// Keep track of it in our map
			m_actionMap.addParameter(p_newParameter);
		}
		else
		{
			Logger.w(TAG, "adding node parameter to null action");
		}

	}
	
	/**
	 * Create a new action parameter depdentend on another parameter
	 * 
	 * @param p_dependentParameter
	 * @return
	 */
	public ActionParameter createActionParameter(short p_actionId, IParameter p_dependentParameter)
	{
	   ActionParameter param = new ActionParameter(getUniqueParameterId(), 
	           p_actionId, 
	           p_dependentParameter.getName(), 
	           p_dependentParameter.getDataType(), 
	           p_dependentParameter.getValidationType(), 
	           p_dependentParameter.getMinimumValue(), 
	           p_dependentParameter.getMaximumValue(), 
	           p_dependentParameter.getMaxStringLength(), 
	           p_dependentParameter.getEnumValueMap())  ;
	   
	   Logger.v(TAG, "creating new action parameter " + param.getParameterIdString() + 
	           " from " + p_dependentParameter.getParameterIdString());
	   
	   // Set the initial parameter as its dependent
	   param.setDependentParamId(p_dependentParameter.getParameterId());
	   
	   // Add it to the map
	   addActionParameter(param);
	   
	   return param;
	}
	
	/**
	 * Simply add a parameter to the map
	 * @param p_parameter
	 */
	public void addActionParameter(ActionParameter p_parameter)
	{
	    m_actionMap.addParameter(p_parameter);
	}

	/**
	 * @param p_parameterId
	 * @param p_paramIndex
	 * @param p_functionId
	 * @param p_nodeId
	 * @param p_strName
	 * @param p_dataType
	 * @param p_paramValidataionType
	 * @param p_minimumValue
	 * @param p_maximumValue
	 * @param p_maxStringLength
	 * @param p_isSigned
	 * @param p_dctEnumValueByName
	 * @return
	 */
	public NodeParameter addNodeParameter(
			INodeAction p_action,
			int p_paramIndex, 
			int p_functionId, 
			long p_nodeId,
			String p_strName,
			EsnDataTypes p_dataType,
			EsnParamValidationTypes p_paramValidataionType,
			long p_minimumValue,
			long p_maximumValue,
			int p_maxStringLength,
			HashMap<Integer, String> p_dctEnumValueByName)
	{
		Logger.i(TAG, String.format("adding node parameter %s for node %X", p_strName, p_nodeId));

		NodeParameter param = new NodeParameter(
				getUniqueParameterId(), 
				p_action.getActionId(), 
				p_paramIndex, 
				p_functionId, 
				p_nodeId, 
				p_strName, 
				p_dataType, 
				p_paramValidataionType, 
				p_minimumValue, 
				p_maximumValue, 
				p_maxStringLength, 
				p_dctEnumValueByName);

		// Add the parameter to the action
		p_action.addParameter(param);

		// Keep track of it in our map
		m_actionMap.addParameter(param);

		return param;
	}
	
	/**
	 * @return a newly created sequence action
	 */
	public SequenceAction addSequenceAction()
	{
	    
	    // Create the new sequence action
	    SequenceAction newAction = new SequenceAction(
	            getUniqueActionId(),
	            EsnDataTypes.VOID);
	    
	    // Add it to the user actions
	    addSequenceAction(newAction.getActionId(), newAction);
	    
	    // Send out the update
	    sendUserActionListUpdate();
	    
	    return newAction;
	}
	
	/**
	 * Add the sequence action
	 * 
	 * @param p_actionId
	 * @param p_action
	 */
	private void addSequenceAction(short p_actionId, SequenceAction p_action)
	{
        m_actionMap.addUserAction(p_action);
        
	    Logger.i(TAG, "New sequence action created: " + p_action.getActionIdString());
	}
	
	/**
	 * @return a list of the user actions
	 */
	public IAction[] getUserActions()
	{
	    return m_actionMap.getUserActions();
	}

	/**
	 * Get a parameter based on its Id
	 * 
	 * @param p_parameterId
	 * @return
	 */
	public ParameterBase getParameter(short p_parameterId)
	{
		return m_actionMap.getParameter(p_parameterId);
	}

	/**
	 * Get an action based on its Id
	 * 
	 * @param p_actionId
	 * @return
	 */
	public IAction getAction(short p_actionId)
	{
		return m_actionMap.getAction(p_actionId);
	}

	/**
	 * Execution an action
	 * 
	 * @param p_actionId
	 * @return true if the Action was executed
	 */
	public ActionRequest executeAction(short p_actionId)
	{

		IAction action = m_actionMap.getAction(p_actionId);

		return executeAction(action, C_I_ACTION_TIMEOUT);
	}
	
	/**
	 * Execution an action with a specified timeout
	 * 
	 * @param p_actionId
	 * @return true if the Action was executed
	 */
	public ActionRequest executeAction(short p_actionId, long p_timeoutMilliseconds)
	{

		IAction action = m_actionMap.getAction(p_actionId);

		return executeAction(action, p_timeoutMilliseconds);
	}

	/**
	 * @param p_action
	 * @param p_timeoutMilliseconds
	 * @return
	 */
	private ActionRequest executeAction(IAction p_action, long p_timeoutMilliseconds)
	{

		ActionRequest request = new ActionRequest(p_action, p_timeoutMilliseconds);

		if (p_action != null)
		{
			Logger.v(TAG, "executing " + p_action.getActionIdString());
			
			// Create a new action runner thread and put it in the pool
			m_actionExecutor.execute(
					new ActionRunnerThread(request, m_nodeManager, this, m_msgDispatcher) );
		}
		else
		{
			Logger.w(TAG, "executing null action");
		}

		return request;
	}

	/**
	 * Class to handle Actions that were rejected because the thread pool is full
	 */
	public class RejectedActionExecutionHandler implements RejectedExecutionHandler
	{
		public static final String TAG = "RejectedActionExecutionHandler";

		@Override
		public void rejectedExecution(Runnable arg0, ThreadPoolExecutor arg1)
		{
			// Handle this better
			Logger.e(TAG, "Rejecting actions because the threadpool is full");
		}

	}
	
    /**
     * Broadcast a node list update
     */
    public void sendUserActionListUpdate()
    {
        MsgSystemUserActionListTransmit msg = 
            new MsgSystemUserActionListTransmit(m_broadcastNode, getUserActions());
        m_msgDispatcher.sendMessage(msg);
    }

    /**
     * Handle a user action list receive message
     * 
     * @param p_msg
     */
    public void addMsgUserActionListRecieve(MsgSystemUserActionListReceive p_msg)
    {
      if(p_msg.getActionType().equals(SequenceAction.TYPE))
      {
          SequenceAction action = new SequenceAction(p_msg.getXml());
          addSequenceAction(action.getActionId(), action);
      }
      else
      {
          Logger.w(TAG, "user action list receive message for unknown message type: " + p_msg.getActionType());
      }
    }

}
