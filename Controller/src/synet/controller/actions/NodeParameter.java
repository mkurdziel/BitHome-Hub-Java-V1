package synet.controller.actions;

import java.util.HashMap;

import nu.xom.Element;

import synet.controller.Protocol.EsnDataTypes;
import synet.controller.Protocol.EsnParamValidationTypes;
import synet.controller.utils.Logger;

public class NodeParameter extends ActionParameter implements Comparable<NodeParameter>, 
    INodeParameter
{
    public static final String TAG = "NodeParameter";
    
	public int m_paramIndex;
	public int m_functionId;
	public long m_nodeId;
	public String m_description = null;
	
	/**
	 * Default constructor
	 * 
	 * @param p_parameterId
	 * @param p_functionId
	 * @param p_nodeId
	 */
	public NodeParameter(
			short p_parameterId,
			short p_actionId,
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
		super(  p_parameterId,
				p_actionId,
				p_strName, 
				p_dataType, 
				p_paramValidataionType,
				p_minimumValue, 
				p_maximumValue, 
				p_maxStringLength, 
				p_dctEnumValueByName);
		
		m_paramIndex = p_paramIndex;
		m_functionId = p_functionId;
		m_nodeId = p_nodeId;
	}
	
	/**
	 * Deserialization constructor
	 * 
	 * @param p_xml
	 * @param p_actionId
	 * @param p_nodeId
	 * @param p_functionIndex
	 * @param p_paramIndex
	 */
	public NodeParameter(
			Element p_xml, 
			short p_actionId, 
			long p_nodeId, 
			int p_functionIndex, 
			int p_paramIndex)
	{
		super(p_xml, p_actionId);
		
		deserialize(p_xml);
		
		m_functionId = p_functionIndex;
		m_paramIndex = p_paramIndex;
		m_nodeId = p_nodeId;
	}
	
	/**
	 * @return the parameter ID, specified by the node
	 */
	public int getParameterIndex() {
		return m_paramIndex;
	}
	
	/**
	 * @return the function ID, specified by the node
	 */
	public int getFunctionIndex() {
		return m_functionId;
	}
	
	/**
	 * @return the node ID that owns this parameter
	 */
	public long getNodeId() {
		return m_nodeId;
	}
	
	/* (non-Javadoc)
	 * @see synet.controller.ParameterBase#getDescription()
	 */
	public String getDescription()
	{
		if (m_description == null)
		{
			StringBuilder sbInfoText = new StringBuilder();
			sbInfoText.append(m_functionId);
			sbInfoText.append("-");
			sbInfoText.append(m_paramIndex);
			sbInfoText.append(" ");

			sbInfoText.append(super.getDescription());

			m_description = sbInfoText.toString();
		}
		return m_description;
	}

	@Override
	public int compareTo(NodeParameter p_arg)
	{
		return getParameterIndex() - p_arg.getParameterIndex();
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
     * Deep compare this parameter to another parameter
     * 
     * @param p_param
     * @return
     */
    public boolean isEqualTo(NodeParameter p_param, boolean p_compareId)
    {
        boolean retVal = super.isEqualTo(p_param, p_compareId);
        
        retVal &= getParameterIndex() == p_param.getParameterIndex();
        retVal &= getFunctionIndex() == p_param.getFunctionIndex();
        retVal &= getNodeId() == p_param.getNodeId();
        
        return retVal;
    }

    /* (non-Javadoc)
     * @see synet.controller.actions.INodeParameter#isEqualTo(synet.controller.actions.INodeParameter, boolean)
     */
    @Override
    public boolean isEqualTo(INodeParameter p_param, boolean p_compareId)
    {
        if (p_param instanceof NodeParameter)
        {
            return isEqualTo((NodeParameter)p_param, p_compareId);
        }
        return false;
    }
}
