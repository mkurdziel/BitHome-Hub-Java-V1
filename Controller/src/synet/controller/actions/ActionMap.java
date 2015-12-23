package synet.controller.actions;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Map-based storage for Actions with helper methods. 
 * Actions need to be accessed efficiently so this class
 * can take care of the bookkeeping
 * 
 * @author kur57360
 *
 */
public class ActionMap {

    // List of all actions by Action ID
    // This allows fast lookup of Action when executing
    HashMap<Short, IAction> m_actionById = new HashMap<Short, IAction>();

    // List of anonymous actions that were note associated with
    // a node but also are not a user-created action
    // These are created so there is only one instance of them in the system
    // and they can be used all over the place
    ArrayList<IAction> m_anonActionList = new ArrayList<IAction>();

    // List of actions that were created by the user
    ArrayList<IAction> m_userActionList = new ArrayList<IAction>();

    // Map of actions by node ID
    HashMap<Long, ArrayList<INodeAction>> m_nodeActionMap = new HashMap<Long, ArrayList<INodeAction>>();

    // Map of all parameters by parameter ID
    HashMap<Short, ParameterBase> m_parameterMap = new HashMap<Short, ParameterBase>();

    /**
     * Get an Action based on its Action ID. If no Action is found with that
     * ID, null is returned;
     * 
     * @param m_actionId
     * @return
     */
    public IAction getAction(short p_actionId)
    {
        return m_actionById.get(p_actionId);
    }

    /**
     * Add a parameter
     * 
     * @param p_parameter
     */
    public void addParameter(ParameterBase p_parameter)
    {
        m_parameterMap.put(p_parameter.getParameterId(), p_parameter);
    }

    /**
     * @param p_parameterId
     * @return the parameter for the parameter ID
     */
    public ParameterBase getParameter(short p_parameterId)
    {
        return m_parameterMap.get(p_parameterId);
    }

    /**
     * Add an action to the map
     * 
     * @param p_action
     */
    public void add(IAction p_action) {
        m_actionById.put(p_action.getActionId(), p_action);
    }

    /**
     * Add a user-created action
     * 
     * @param p_action
     */
    public void addUserAction(IAction p_action)
    {
        m_userActionList.add(p_action);
        add(p_action);
    }

    /**
     * @return a list of user-created actions
     */
    public IAction[] getUserActions()
    {
        return m_userActionList.toArray(new IAction[m_userActionList.size()]);
    }

    /**
     * Add a node action 
     * 
     * @param p_action
     */
    public void addNodeAction(INodeAction p_action)
    {
        add(p_action);

        // If there is no entry for this node, create the initial list
        if (!m_nodeActionMap.containsKey(p_action.getNodeId()))
        {
            m_nodeActionMap.put(p_action.getNodeId(), new ArrayList<INodeAction>());
        }

        m_nodeActionMap.get(p_action.getNodeId()).add(p_action);
    }


    /**
     * Return the node actions for a particular node ID
     * 
     * @param p_nodeId
     * @return
     */
    public ArrayList<INodeAction> getNodeActions(long p_nodeId)
    {
        if (m_nodeActionMap.containsKey(p_nodeId))
        {
            return m_nodeActionMap.get(p_nodeId);
        }
        return new ArrayList<INodeAction>();
    }

    /**
     * Remove a node Action
     * 
     * @param p_nodeId
     * @param p_replacedAction
     */
    public void removeNodeAction(long p_nodeId, INodeAction p_replacedAction)
    {
        ArrayList<INodeAction> actions = m_nodeActionMap.get(p_nodeId);
        if (actions != null)
        {
            actions.remove(p_replacedAction);
        }
    }
}