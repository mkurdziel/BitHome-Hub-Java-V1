package synet.controller.actions;

import java.util.HashMap;

import nu.xom.Attribute;
import nu.xom.Element;

import synet.controller.Protocol.EsnDataTypes;
import synet.controller.Protocol.EsnParamValidationTypes;
import synet.controller.utils.Logger;
import synet.controller.utils.XmlUtils;

public class ActionParameter extends ParameterBase implements IActionParameter
{
	private static final String TAG = "ActionParameter";
	private static final String C_STR_XML_VALUE = "value";
    private static final String C_STR_XML_PARAMTYPE = "paramType";

    /**
	 * Enumeration describing the parameter type
	 */
	public enum EsnActionParameterType
	{
		// Parameter needs to be set by the user
		INPUT,
		// Parameter is defined as a constant
		CONSTANT,
		// Parameter is dependent upon another parameter's value
		DEPENDENT,
		// Parameter is internally set
		INTERNAL
	}

	private	short m_actionId;
	private EsnActionParameterType m_parameterType = EsnActionParameterType.INPUT;
	
	// Caching
	private ParameterBase m_dependentParam = null;
	
	/**
	 * @param p_actionId
	 * @param p_strName
	 * @param p_dataType
	 * @param p_paramValidataionType
	 * @param p_minimumValue
	 * @param p_maximumValue
	 * @param p_maxStringLength
	 * @param p_isSigned
	 * @param p_dctEnumValueByName
	 */
	public ActionParameter(
			short p_parameterId,
			short p_actionId,
			String p_strName, 
			EsnDataTypes p_dataType,
			EsnParamValidationTypes p_paramValidataionType,
			long p_minimumValue, 
			long p_maximumValue, 
			int p_maxStringLength,
			HashMap<Integer, String> p_dctEnumValueByName)
	{
		super(p_parameterId,
				p_strName,
				p_dataType,
				p_paramValidataionType,
				p_minimumValue,
				p_maximumValue,
				p_maxStringLength,
				p_dctEnumValueByName);
		
		m_actionId = p_actionId;
		m_parameterType = EsnActionParameterType.INPUT;
	}
	
	/**
	 * Deserialization constructor
	 * 
	 * @param p_xml
	 * @param p_actionId
	 */
	public ActionParameter(Element p_xml, Short p_actionId)
	{
		super(p_xml);
		this.deserialize(p_xml);
		m_actionId = p_actionId;
	}
	
	/**
	 * @return the Action ID this parameter is associated with
	 */
	public short getActionId()
	{
		return m_actionId;
	}
	
	/**
	 * @param p_actionId
	 */
	public void setActionId(short p_actionId)
	{
	    m_actionId = p_actionId;
	}
	
	/**
	 * @return the Action parameter type
	 */
	public EsnActionParameterType getParameterType()
	{
		return m_parameterType;
	}
	
	/**
	 * Set the parameter type
	 * 
	 * @param p_type
	 */
	public void setParameterType(EsnActionParameterType p_type)
	{
	    m_parameterType = p_type;
	}
	
	/* (non-Javadoc)
	 * @see synet.controller.actions.ActionBase#serialize()
	 */
	public Element serialize()
	{
		Element base = super.serialize();
		
		base.addAttribute(new Attribute(C_STR_XML_PARAMTYPE, getParameterType().toString()));
		// If it is a constant, serialize the value
		if (m_parameterType == EsnActionParameterType.CONSTANT)
		{
		   base.addAttribute(new Attribute(C_STR_XML_VALUE, getStrValue())); 
		}
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
		// Deserialize the base
        m_parameterType = EsnActionParameterType.valueOf(XmlUtils.getXmlAttributeString(p_xml, C_STR_XML_PARAMTYPE, m_parameterType.toString()));
        
        if (m_parameterType == EsnActionParameterType.CONSTANT)
        {
            String value = XmlUtils.getXmlAttributeString(p_xml, C_STR_XML_VALUE);
            if (value != null && !value.equals(""))
            {
                setValue(value);
            }
        }
		return true;
	} 
	
	 /** 
     * Deep compare this parameter to another parameter
     * 
     * @param p_param
     * @return
     */
    public boolean isEqualTo(ActionParameter p_param, boolean p_compareId)
    {
        boolean retVal = true;
        
        retVal &= super.isEqualTo(p_param, p_compareId);
        
        if (p_compareId)
        {
            retVal &= getActionId() == p_param.getActionId();
        }
        retVal &= getParameterType() == p_param.getParameterType();
        
        return retVal;
    }
    
    /* (non-Javadoc)
     * @see synet.controller.ParameterBase#getStrValue()
     */
    @Override
    public String getStrValue()
    {
        if (m_parameterType == EsnActionParameterType.DEPENDENT)
        {
            // TODO: handle changing dependent parameters
            if (m_dependentParam == null)
            {
                // Cache this duder
                m_dependentParam = ActionManager.getInstance().getParameter(getDependentParamId());
            }
            
            if (m_dependentParam != null)
            {
                Logger.w(TAG, "getting dependent param value with no dependent param");
                return m_dependentParam.getStrValue();
            }
        }
        return super.getStrValue();
    }

    /* (non-Javadoc)
     * @see synet.controller.actions.IParameter#isEqualTo(synet.controller.actions.IParameter, boolean)
     */
    @Override
    public boolean isEqualTo(IParameter p_param, boolean p_compareId)
    {
        if (p_param instanceof ActionParameter)
        {
            return isEqualTo((ActionParameter)p_param, p_compareId);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see synet.controller.actions.IActionParameter#isEqualTo(synet.controller.actions.IActionParameter, boolean)
     */
    @Override
    public boolean isEqualTo(IActionParameter p_param, boolean p_compareId)
    {
        if (p_param instanceof ActionParameter)
        {
            return isEqualTo((ActionParameter)p_param, p_compareId);
        }
        return false;
    }
}
