/**
 * 
 */
package synet.controller.actions;

import java.util.ArrayList;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;
import synet.controller.NodeManager;
import synet.controller.Protocol.EsnDataTypes;
import synet.controller.messaging.MsgDispatcher;
import synet.controller.utils.Logger;
import synet.controller.utils.XmlUtils;

/**
 * @author mkurdziel
 *
 */
public class SequenceAction extends ActionBase implements IAction
{
    private static final String C_STR_XML_PARAMETER = "parameter";
    private static final String C_STR_XML_PARAMETERS = "parameters";
    private static final String C_STR_XML_ACTIONID = "actionId";
    private static final String C_STR_XML_ACTIONITEM = "actionItem";
    private static final String C_STR_XML_ACTIONITEMS = "actionItems";
    private final static String TAG = "SequenceAction";
    public final static String TYPE = "Sequence";

    private ArrayList<ActionItem> m_actions;

    /**
     * @param p_actionId
     * @param p_returnType
     */
    public SequenceAction(short p_actionId, EsnDataTypes p_returnType)
    {
        super(p_actionId, p_returnType);

        m_actions = new ArrayList<ActionItem>();
    }

    /**
     * @param p_xml
     */
    public SequenceAction(Element p_xml)
    {
        super(p_xml);
        
        deserialize(p_xml);
    }

    /**
     * Add the action to the sequence action
     * 
     * @param p_action
     */
    public void addAction(IAction p_action)
    {
        Logger.v(TAG, p_action.getActionIdString() + " added to sequence action " + getActionIdString());
        ActionItem actionItem = new ActionItem(p_action);
        m_actions.add(actionItem);

        // For each parameter in the action, create a local action parameter that
        // is linked to the parent parmeter
        for(IParameter param : p_action.getParameters())
        {
            ActionParameter actionParameter = ActionManager.getInstance().createActionParameter(getActionId(), param);
            actionItem.addParameter(actionParameter);
        }
    }

    /**
     * remove the action from the sequence action
     * 
     * @param p_action
     */
    public void removeAction(ActionItem p_actionItem)
    {
        Logger.v(TAG, p_actionItem.getAction().getActionIdString() + " removed from sequence action " + getActionIdString());
        m_actions.remove(p_actionItem);
    }


    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#execute(synet.controller.NodeManager, synet.controller.actions.ActionManager, synet.controller.messaging.MsgDispatcher, long)
     */
    @Override
    public boolean execute(NodeManager p_nodeManager,
            ActionManager p_actionManager, MsgDispatcher p_msgDispatcher,
            long p_timeoutMilliseconds)
    {
        ActionManager am = ActionManager.getInstance();

        boolean retVal = true;
        for(ActionItem actionItem : m_actions)
        {
            IAction action = actionItem.getAction();
            for(ActionParameter param : actionItem.getParameters())
            {
                am.getParameter(param.getDependentParamId()).setValue(param.getStrValue());
            }

            retVal &= action.execute(p_nodeManager, p_actionManager, p_msgDispatcher, p_timeoutMilliseconds);
        }
        return retVal;
    }

    /**
     * Internal class representing an action item in the sequence
     */
    private class ActionItem
    {
        private IAction m_action;
        private ArrayList<ActionParameter> m_params;

        public ActionItem(IAction p_action)
        {
            m_action = p_action;
            m_params = new ArrayList<ActionParameter>();
        }

        /**
         * @return the action for this item
         */
        public IAction getAction()
        {
            return m_action;
        }

        /**
         * @return the string values of the parameters
         */
        public ArrayList<ActionParameter> getParameters()
        {
            return m_params;
        }

        /**
         * @param param
         */
        public void addParameter(ActionParameter param)
        {
            m_params.add(param);
        }
    }


    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#deserialize(nu.xom.Element)
     */
    private boolean deserialize(Element p_xml)
    {
        m_actions = new ArrayList<ActionItem>();

        Element actionsElement = p_xml.getFirstChildElement(C_STR_XML_ACTIONITEMS);

        if (actionsElement != null)
        {
            Elements actionElements = actionsElement.getChildElements(C_STR_XML_ACTIONITEM);
            for (int i=0; i<actionElements.size(); ++i)
            {
                Element actionElement = actionElements.get(i);
                short actionId = XmlUtils.getXmlAttributeShort(actionElement, C_STR_XML_ACTIONID);
                IAction action = ActionManager.getInstance().getAction(actionId);
                if (action != null)
                {
                    ActionItem actionItem = new ActionItem(action); 
                    m_actions.add(actionItem);

                    // Deserialize all parameters
                    Element paramsElement = actionElement.getFirstChildElement(C_STR_XML_PARAMETERS);
                    if (paramsElement != null)
                    {
                        Elements paramElements = paramsElement.getChildElements(C_STR_XML_PARAMETER);
                        for (int j=0; j<paramElements.size(); ++j)
                        {
                            Element paramElement = paramElements.get(j);
                            ActionParameter param = new ActionParameter(paramElement, getActionId());
                            actionItem.addParameter(param);
                            ActionManager.getInstance().addActionParameter(param);
                        }
                    }
                }
                else
                {
                    Logger.w(TAG, "deserializing null action");
                }
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#serialize()
     */
    @Override
    public Element serialize()
    {
        Element element = super.serialize();

        element.addAttribute(new Attribute("actionType", this.getActionType()));

        // Serialize each action in the sequence
        Element actionsElement = new Element(C_STR_XML_ACTIONITEMS);

        for(ActionItem actionItem : m_actions)
        {
            Element actionElement = new Element(C_STR_XML_ACTIONITEM);
            IAction action = actionItem.getAction();
            actionElement.addAttribute(new Attribute(C_STR_XML_ACTIONID, action.getActionIdString()));
            Element parametersElement = new Element(C_STR_XML_PARAMETERS);
            actionElement.appendChild(parametersElement);

            for(ActionParameter param : actionItem.getParameters())
            {
                parametersElement.appendChild(param.serialize()); 
            }

            actionsElement.appendChild(actionElement);
        }

        element.appendChild(actionsElement);

        return element;
    }

    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#getActionType()
     */
    @Override
    public String getActionType()
    {
        return TYPE;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append(String.format("%s[%s] - %s\n", getName(), getActionType(), getActionIdString()));
        int i=1;
        for(ActionItem actionItem : m_actions)
        {
            sb.append(String.format("\t%d) ActionItem: %s - %s\n", i++, actionItem.getAction().getActionIdString(), actionItem.getAction().getName()));
            for(ActionParameter parameter : actionItem.getParameters())
            {
                sb.append(String.format("\t\t%s: %s\n", parameter.getParameterIdString(), parameter.toString()));
            }
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#getParameters()
     */
    @Override
    public ActionParameter[] getParameters()
    {
        ArrayList<ActionParameter> params = new ArrayList<ActionParameter>();

        for(ActionItem actionItem : m_actions)
        {
            params.addAll(actionItem.getParameters());
        }

        return params.toArray(new ActionParameter[params.size()]);
    }

    @Override
    public boolean isEqualTo(IAction p_action, boolean p_compareId)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void addParameter(IParameter p_parameter)
    {
        // TODO Auto-generated method stub

    }
}
