package synet.controller.actions;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nu.xom.Attribute;
import nu.xom.Element;

import synet.controller.NodeManager;
import synet.controller.Protocol.EsnDataTypes;
import synet.controller.actions.ActionParameter.EsnActionParameterType;
import synet.controller.messaging.MsgDispatcher;
import synet.controller.utils.Logger;
import synet.controller.utils.XmlUtils;

/**
 * Base class for all Actions
 * 
 */
public abstract class ActionBase implements IAction {
	private static final String TAG = "ActionBase";
	private static final String STR_NAME = "NEW ACTION";

	public static final String C_STR_XML_DESCRIPTION = "description";
	public static final String C_STR_XML_NAME = "name";
	public static final String C_STR_XML_ID = "id";
	public static final String C_STR_XML_ACTION = "action";
    public static final String C_STR_XML_RETURNTYPE = "returnType";
    public static final String C_STR_XML_ACTIONTYPE = "actionType";
    
	private short m_actionId;
	private String m_name = STR_NAME;
	private String m_executeErrorString;
	private String m_description = "";
	private int m_numParameters = 0;
	private ArrayList<IActionParameter> m_parameters = new ArrayList<IActionParameter>();
	private Lock m_actionLock = new ReentrantLock(true);
    private EsnDataTypes m_returnType = EsnDataTypes.VOID;
    private String m_stringReturnValue = "";


	/**
	 * Protected initializer to be used by the ActionManager
	 * 
	 * @param m_actionId
	 */
	protected ActionBase(short p_actionId, EsnDataTypes p_returnType)
	{
		this();
        m_returnType = p_returnType;
		m_actionId = p_actionId;
	}

	/**
	 * Deserialization constructor
	 * 
	 * @param p_xml
	 */
	protected ActionBase(Element p_xml)
	{
		deserialize(p_xml);
	}

	/**
	 * Private constructor so we always get an action ID
	 */
	private ActionBase()
	{
	}
    
    /**
     * Set the return value for the action
     * 
     * @param p_value
     */
    public void setStringReturnValue(String p_value)
    {
        m_stringReturnValue = p_value;
    }
    
    /**
     * @return the string return value
     * TODO: integer return value
     */
    public String getStringReturnValue()
    {
        return m_stringReturnValue;
    }
    
    /**
     * @return the return type of the action
     */
    public EsnDataTypes getReturnType()
    {
        return m_returnType;
    }

	/**
	 * @return a list of parameters associated with this Action
	 */
	public IActionParameter[] getParameters()
	{
		return m_parameters.toArray(new ActionParameter[m_parameters.size()]);
	}

	/**
	 * Set the parameter for a specific index
	 * 
	 * @param p_index
	 * @param p_parameter
	 */
	public void addParameter(IActionParameter p_parameter)
	{
		Logger.v(TAG, getActionIdString() + ": adding parameter " + p_parameter.getName());

		m_parameters.add(p_parameter);
	}

	/**
	 * @param p_index
	 * @return the parameter at the index. Returns null if none set.
	 */
	public IActionParameter getParameter(int p_index)
	{
		return m_parameters.get(p_index);
	}

	/**
	 * Clear all the parameters for this Action
	 */
	public void clearParameters()
	{
		m_parameters.clear();
	}

	/**
	 * Set the name of the Action
	 * 
	 * @param p_name
	 */
	public void setName(String p_name)
	{
		m_name = p_name;
	}

	/**
	 * @return the name of the Action
	 */
	public String getName()
	{
		return m_name;
	}

	/**
	 * @return the Action ID
	 */
	public short getActionId()
	{
		return m_actionId;
	}

	/**
	 * @return the description of the Action
	 */
	public String getDescription()
	{
		return m_description;
	}

	/**
	 * Set the number of parameters in this Action
	 * 
	 * @param p_numParameters
	 */
	public void setNumParameters(int p_numParameters)
	{
		m_numParameters = p_numParameters;

		m_parameters = new ArrayList<IActionParameter>(p_numParameters);
	}

	/**
	 * @return the number of parameters that should be in this function
	 */
	public int getNumParameters()
	{
		return m_numParameters;
	}
	
	/**
	 * TODO: fix this!!!!!
	 * 
	 * @return the count of the parameter storage
	 */
	public int getParameterCount()
	{
	   return m_parameters.size(); 
	}


	/**
	 * @return the Action ID as a formatted string
	 */
	public String getActionIdString()
	{
		return String.format("0x%x", m_actionId);
	}
	
	/**
	 * Prepare and lock the action for execution
	 */
	public final void prepareExecute()
	{
		m_executeErrorString = "";
		// Lock so this action can only be executed once at a time
		m_actionLock.lock();
	}

	/**
	 * @return true if the Action executed successfully
	 */
	public abstract boolean execute(
			NodeManager p_nodeManager, 
			ActionManager p_actionManager,
			MsgDispatcher p_msgDispatcher,
			long p_timeoutMilliseconds);
	
	/**
	 * Finish and unlock the execution
	 */
	public final void finishExecute()
	{
		// Release the action to the system
		m_actionLock.unlock();
	}
	
	/**
	 * @return an optional execution error string
	 */
	public String getExecuteErrorString()
	{
		return m_executeErrorString;
	}
	
	/**
	 * Set the execution error string
	 * 
	 * @param p_executeErrorString
	 */
	public void setExecuteErrorString(String p_executeErrorString)
	{
		m_executeErrorString = p_executeErrorString;
	}

	
	/**
	 * @return
	 */
	public Element serialize()
	{
		Element actionElement = new Element(C_STR_XML_ACTION);

		// Add the other attributes
		Attribute idAttribute = new Attribute(C_STR_XML_ID, getActionIdString());
		Attribute nameAttribute = new Attribute(C_STR_XML_NAME, getName());
		Attribute descAttribute = new Attribute(C_STR_XML_DESCRIPTION, String.valueOf(getDescription()));
        Attribute returnAttribute = new Attribute(C_STR_XML_RETURNTYPE, String.valueOf(getReturnType()));

		actionElement.addAttribute(idAttribute);
		actionElement.addAttribute(nameAttribute);
		actionElement.addAttribute(descAttribute);
		actionElement.addAttribute(returnAttribute);

		return actionElement;
	}

	/**
	 * Deserialize the Action from XML
	 * 
	 * @param p_xml
	 * @return
	 */
	private boolean deserialize(Element p_xml)
	{
		if (p_xml == null)
		{
			Logger.w(TAG, "deserialization failed. Null XML");
			return false;
		}

		if (p_xml.getLocalName().compareTo(C_STR_XML_ACTION) == 0)
		{
			m_actionId = XmlUtils.getXmlAttributeShort(p_xml, C_STR_XML_ID);
			if (m_actionId != 0)
			{
				m_name = XmlUtils.getXmlAttributeString(p_xml, C_STR_XML_NAME);
				m_description = XmlUtils.getXmlAttributeString(p_xml, C_STR_XML_DESCRIPTION);
				m_returnType = XmlUtils.getXmlAttributeDataType(p_xml, C_STR_XML_RETURNTYPE);
				
				return true;
			}
			else
			{
				Logger.w(TAG, "deserialization failed. Invalid ID");
				return false;
			}
		}
		else
		{
			Logger.w(TAG, "deserialization failed. Incorrect root node: " + p_xml.getLocalName());
		}

		return false;
	}
	
	/* (non-Javadoc)
	 * @see synet.controller.actions.IAction#isEqualTo(synet.controller.actions.IAction, boolean)
	 */
	public boolean isEqualTo(IAction p_action, boolean p_compareId)
	{
	    return isEqualTo((ActionBase)p_action, p_compareId);
	}
	
	/**
	 * @param p_action
	 * @param p_compareId
	 * @return
	 */
	public boolean isEqualTo(ActionBase p_action, boolean p_compareId) 
	{
	    boolean retVal = true;
	    
	    /*
	     *     private short m_actionId;
    private String m_name = STR_NAME;
    private String m_executeErrorString;
    private String m_description = "";
    private int m_numParameters = 0;
    private ArrayList<ActionParameter> m_parameters = new ArrayList<ActionParameter>();
    private Lock m_actionLock = new ReentrantLock(true);
    private EsnDataTypes m_returnType;
    private String m_stringReturnValue = "";
	     */
	    
	    if (p_compareId)
	    {
	        retVal &= m_actionId == p_action.getActionId();
	    }
	    retVal &= getName().equals(p_action.getName());
	    retVal &= getDescription().equals(p_action.getDescription());
	    retVal &= getNumParameters() == p_action.getNumParameters();
	    retVal &= getReturnType() == p_action.getReturnType();
	    retVal &= getParameterCount() == p_action.getParameterCount();
	    
	    for (int i=1; i<=getNumParameters(); ++i)
	    {
	        IActionParameter p1 = getParameter(i);
	        IActionParameter p2 = p_action.getParameter(i);
	        
	        if (!(p2 != null && p1.isEqualTo(p2, p_compareId)))
	        {
	            retVal = false;
	            break;
	        }
	    }
	    return retVal;
	}
	
	/**
	 * @return a String identifier of the action type
	 */
	public abstract String getActionType();
	
    
    /**
     * @return all the input action parameters
     */
    public IActionParameter[] getInputParameters()
    {
        IActionParameter[] params = getParameters();
        ArrayList<IActionParameter> inputParams = new ArrayList<IActionParameter>();
        
        for(IActionParameter param : params)
        {
            if (param.getParameterType() == EsnActionParameterType.INPUT)
            {
                inputParams.add(param);
            }
        }
        
        return inputParams.toArray(new ActionParameter[inputParams.size()]);
    }
}
