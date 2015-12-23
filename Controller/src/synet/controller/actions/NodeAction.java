package synet.controller.actions;

import java.util.Arrays;
import java.util.HashMap;

import nu.xom.Attribute;
import nu.xom.Element;

import synet.controller.NodeManager;
import synet.controller.Protocol.EsnDataTypes;
import synet.controller.messaging.MsgDispatcher;
import synet.controller.messaging.messages.MsgFunctionTransmit;
import synet.controller.nodes.NodeBase;
import synet.controller.utils.Logger;
import synet.controller.utils.XmlUtils;

/**
 * This Action is a one-to-one mapping to the functions available on a Node;
 * 
 */
public class NodeAction extends ActionBase implements INodeAction {
    private static final String TAG = "NodeAction";
    private static final String TYPE = "Node";


    public long m_nodeId;
    public int m_functionId;

    private HashMap<Integer, INodeParameter> m_parameters = 
        new HashMap<Integer, INodeParameter>();

    /**
     * Protected constructor to be used by the Action Manager
     * TODO: p_paramTypes is unused. Is it needed?
     * 
     * @param p_actionId
     * @param p_nodeId
     * @param m_functionId
     */
    protected NodeAction(
            short p_actionId, 
            long p_nodeId, 
            int p_functionId,
            String p_name,
            EsnDataTypes p_returnType,
            int p_numParams,
            HashMap<Integer, EsnDataTypes> p_paramTypes )
    {
        super(p_actionId, p_returnType);
        m_functionId = p_functionId;
        m_nodeId = p_nodeId;

        setName(p_name);
        setNumParameters(p_numParams);
    }

    /**
     * Deserialization constructor
     * 
     * @param p_xml
     */
    public NodeAction(Element p_xml, long p_nodeId, int p_functionIndex)
    {
        super(p_xml);

        deserialize(p_xml);

        m_nodeId = p_nodeId;
        m_functionId = p_functionIndex;
    }


    /**
     * @return the Node ID this Action belongs to
     */
    public long getNodeId()
    {
        return m_nodeId;
    }

    /**
     * @return the Function ID this Action references
     */
    public int getFunctionId()
    {
        return m_functionId;
    }


    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#getParameter(int)
     */
    @Override
    public INodeParameter getParameter(int p_index)
    {
        return m_parameters.get(p_index);
    }

    /**
     * @param p_parameter
     */
    public void addParameter(INodeParameter p_parameter)
    {
        Logger.v(TAG, getName() + " adding parameter " + p_parameter.getName() + " at index " + p_parameter.getParameterIndex());

        m_parameters.put(p_parameter.getParameterIndex(), p_parameter);

        super.addParameter(p_parameter);
    }


    /**
     * @return the next unknown parameter
     */
    public int getNextUnknownParameter()
    {
        Logger.v(TAG, getName() + " getting next unknown parameter "+m_parameters.size()+":"+getNumParameters());

        for (int i=1; i<=getNumParameters(); ++i)
        {
            if (m_parameters.get(i) == null)
            {
                return i;
            }
        }
        return 0;
    }

    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#execute(synet.controller.messaging.MsgDispatcher)
     */
    @Override
    public boolean execute(
            NodeManager p_nodeManager,
            ActionManager p_actionManager, 
            MsgDispatcher p_msgDispatcher,
            long p_timeoutMilliseconds) {


        NodeBase destNode = p_nodeManager.getNode(this.getNodeId());

        if (destNode != null)
        {

            Logger.v(TAG, String.format("executing action %s %d:%d]", super.getName(), super.getNumParameters(),getParameters().length));

            MsgFunctionTransmit msg = new MsgFunctionTransmit(
                    destNode, 
                    getFunctionId(), 
                    super.getNumParameters(), 
                    getParameters(),
                    getReturnType());

            p_msgDispatcher.sendMessage(msg);

            // Wait to see if the message actually sent
            if (!msg.waitForSend(p_timeoutMilliseconds))
            {
                if (msg.getIsError())
                {
                    setExecuteErrorString(msg.getErrorMsg());
                }
                else
                {
                    setExecuteErrorString("timed-out waiting or send notification");
                }
                return false;
            }
            // Now wait for a response
            else if(msg.getNeedsReturn())
            {
                if(msg.waitForResponse(p_timeoutMilliseconds))
                {
                    // Set the string return value
                    setStringReturnValue(msg.getResponseMsg().getStringValue());
                }
                else
                {
                    setExecuteErrorString("request sent but no response received");
                    return false;
                }
            }

            return true;
        }
        else
        {
            Logger.w(TAG, super.getName() + " accessing invalid node: " + String.format("0x%x", getNodeId()));
        }

        return false;
    }

    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#getParameters()
     */
    public INodeParameter[] getParameters()
    {
        INodeParameter[] params = m_parameters.values().toArray(new NodeParameter[m_parameters.size()]);

        // Sort so that the resulting array is in the right order 
        // based on the parameter index
        Arrays.sort(params);

        return params;
    }

    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#serialize()
     */
    public Element serialize()
    {
        Element base = super.serialize();

        return base;
    }

    /**
     * Deserialize the Action from XML
     * 
     * @param p_xml
     * @return
     */
    private boolean deserialize(Element p_xml)
    {
            return true;
    }              

    /**
     * Update the number of parameters to the current size
     */
    public void updateNumParameters()
    {
        setNumParameters(m_parameters.size());
    }

    /**
     * @return
     */
    public void debugParameters()
    {
        Logger.d(TAG, "Parameter size: " + m_parameters.size());
        for(Integer index : m_parameters.keySet())
        {
            Logger.d(TAG, "Parameter index: " + index + " name: " + m_parameters.get(index).getName());
        }
    }
    
    public int getParameterCount()
    {
        return m_parameters.size();
    }

    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#getActionType()
     */
    @Override
    public String getActionType()
    {
        return TYPE;
    }

    @Override
    public boolean isEqualTo(IAction p_action, boolean p_compareId)
    {
        return isEqualTo((INodeAction)p_action, p_compareId);
    }
    
    /**
     * @param p_action
     * @param p_compareId
     * @return
     */
    public boolean isEqualTo(INodeAction p_action, boolean p_compareId)
    {
        boolean retVal = super.isEqualTo(p_action, p_compareId);
        
        retVal &= m_functionId == p_action.getFunctionId();
        retVal &= m_nodeId == p_action.getNodeId();
        
        return retVal;
    }

    @Override
    public void addParameter(IParameter p_parameter)
    {
        // TODO Auto-generated method stub
        
    }
}
