package synet.controller.actions;

import java.util.ArrayList;

import nu.xom.Attribute;
import nu.xom.Element;
import synet.controller.NodeManager;
import synet.controller.Protocol.EsnDataTypes;
import synet.controller.messaging.MsgDispatcher;
import synet.controller.utils.Logger;

public class MetaNodeAction implements INodeAction
{
    public final static String TAG = "MetaNodeAction";
    private ActionBase m_action;
    private long m_nodeId;
    private int m_actionIndex;
    
    /**
     * Default constructor 
     * 
     * @param p_action
     */
    public MetaNodeAction ( ActionBase p_action, long p_nodeId, int p_actionIndex )
    {
      m_action = p_action;  
      m_nodeId = p_nodeId;
      m_actionIndex = p_actionIndex;
    }
    
    /**
     * @return the action that this meta action is based on
     */
    public ActionBase getAction()
    {
        return m_action;
    }

    @Override
    public void setStringReturnValue(String p_value)
    {
        m_action.setStringReturnValue(p_value);
    }

    @Override
    public String getStringReturnValue()
    {
        return m_action.getStringReturnValue();
    }

    @Override
    public EsnDataTypes getReturnType()
    {
        return m_action.getReturnType();
    }

    @Override
    public void clearParameters()
    {
        m_action.clearParameters();
    }

    @Override
    public void setName(String p_name)
    {
        m_action.setName(p_name);
    }

    @Override
    public String getName()
    {
        return m_action.getName();
    }

    @Override
    public short getActionId()
    {
        return m_action.getActionId();
    }

    @Override
    public String getDescription()
    {
        return m_action.getDescription();
    }

    @Override
    public void setNumParameters(int p_numParameters)
    {
        m_action.setNumParameters(p_numParameters);
    }

    @Override
    public int getNumParameters()
    {
        return m_action.getNumParameters();
    }

    @Override
    public String getActionIdString()
    {
        return m_action.getActionIdString();
    }

    @Override
    public void prepareExecute()
    {
        m_action.prepareExecute();
    }

    @Override
    public boolean execute(NodeManager p_nodeManager,
            ActionManager p_actionManager, MsgDispatcher p_msgDispatcher,
            long p_timeoutMilliseconds)
    {
        return m_action.execute(p_nodeManager, p_actionManager, p_msgDispatcher, p_timeoutMilliseconds);
    }

    @Override
    public void finishExecute()
    {
        m_action.finishExecute();
    }

    @Override
    public String getExecuteErrorString()
    {
        return m_action.getExecuteErrorString();
    }

    @Override
    public void setExecuteErrorString(String p_executeErrorString)
    {
        m_action.setExecuteErrorString(p_executeErrorString);
    }

    @Override
    public boolean isEqualTo(IAction p_action, boolean p_compareId)
    {
        return m_action.isEqualTo(p_action, p_compareId);
    }

    @Override
    public IActionParameter[] getInputParameters()
    {
        return m_action.getInputParameters();
    }

    @Override
    public long getNodeId()
    {
        return m_nodeId;
    }

    @Override
    public int getFunctionId()
    {
        return m_actionIndex;
    }

    @Override
    public INodeParameter getParameter(int p_index)
    {
        IActionParameter parameter = m_action.getParameter(p_index);
        return new MetaNodeParameter(parameter, m_actionIndex, p_index, m_nodeId);
    }

    @Override
    public void addParameter(INodeParameter p_parameter)
    {
        Logger.w(TAG, "Trying to add parameter when we shouldn't be");

    }

    @Override
    public int getNextUnknownParameter()
    {
        return -1;
    }

    @Override
    public INodeParameter[] getParameters()
    {
        IActionParameter[] params = m_action.getParameters();
        ArrayList<MetaNodeParameter> retParams = new ArrayList<MetaNodeParameter>(params.length);
        
        int paramIdx = 0;
        for(IActionParameter param : params)
        {
            retParams.add(new MetaNodeParameter(param, m_actionIndex, ++paramIdx, m_nodeId));
        }
        
        return retParams.toArray(new INodeParameter[retParams.size()]);
    }

    @Override
    public Element serialize()
    {
        Element root = m_action.serialize();
        root.addAttribute(new Attribute(ActionBase.C_STR_XML_ACTIONTYPE, getActionType()));
        return root;
    }

    @Override
    public int getParameterCount()
    {
        return m_action.getParameterCount();
    }

    @Override
    public String getActionType()
    {
        return "Meta";
    }

    @Override
    public void addParameter(IParameter p_parameter)
    {
        // Do Nothing
    }

}
