package synet.controller.actions;

import nu.xom.Element;
import synet.controller.NodeManager;
import synet.controller.Protocol.EsnDataTypes;
import synet.controller.messaging.MsgDispatcher;

public interface IAction
{
    /**
     * Set the return value for the action
     * 
     * @param p_value
     */
    public void setStringReturnValue(String p_value);

    /**
     * @return the string return value
     * TODO: integer return value
     */
    public String getStringReturnValue();

    /**
     * @return the return type of the action
     */
    public EsnDataTypes getReturnType();

    /**
     * @return a list of parameters associated with this Action
     */
    public IParameter[] getParameters();

    /**
     * Set the parameter for a specific index
     * 
     * @param p_index
     * @param p_parameter
     */
    void addParameter(IParameter p_parameter);

    /**
     * @param p_index
     * @return the parameter at the index. Returns null if none set.
     */
    public IParameter getParameter(int p_index);

    /**
     * Clear all the parameters for this Action
     */
    public void clearParameters();

    /**
     * Set the name of the Action
     * 
     * @param p_name
     */
    public void setName(String p_name);

    /**
     * @return the name of the Action
     */
    public String getName();

    /**
     * @return the Action ID
     */
    public short getActionId();

    /**
     * @return the description of the Action
     */
    public String getDescription();

    /**
     * Set the number of parameters in this Action
     * 
     * @param p_numParameters
     */
    public void setNumParameters(int p_numParameters);

    /**
     * @return the number of parameters that should be in this function
     */
    public int getNumParameters();

    /**
     * TODO: fix this!!!!!
     * 
     * @return the count of the parameter storage
     */
    public int getParameterCount();

    /**
     * @return the Action ID as a formatted string
     */
    public String getActionIdString();

    /**
     * Prepare and lock the action for execution
     */
    void prepareExecute();

    /**
     * @return true if the Action executed successfully
     */
    boolean execute(
            NodeManager p_nodeManager, 
            ActionManager p_actionManager,
            MsgDispatcher p_msgDispatcher,
            long p_timeoutMilliseconds);

    /**
     * Finish and unlock the execution
     */
    void finishExecute();

    /**
     * @return an optional execution error string
     */
    String getExecuteErrorString();

    /**
     * Set the execution error string
     * 
     * @param p_executeErrorString
     */
    void setExecuteErrorString(String p_executeErrorString);

    /**
     * @return
     */
    public Element serialize();

    /**
     * @param p_action
     * @return
     */
    public boolean isEqualTo(IAction p_action, boolean p_compareId);

    /**
     * @return a String identifier of the action type
     */
    public String getActionType();

    /**
     * @return all the input action parameters
     */
    public IParameter[] getInputParameters();
}
