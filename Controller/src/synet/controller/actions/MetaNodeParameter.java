package synet.controller.actions;

import java.util.HashMap;

import nu.xom.Element;
import synet.controller.Protocol.EsnDataTypes;
import synet.controller.Protocol.EsnParamValidationTypes;
import synet.controller.actions.ActionParameter.EsnActionParameterType;

public class MetaNodeParameter implements INodeParameter
{
    IActionParameter m_parameter;
    int m_paramIndex;
    int m_actionIndex;
    long m_nodeId;
    
    /**
     * @param p_parameter
     */
    public MetaNodeParameter(IActionParameter p_parameter, int p_actionIndex, int p_paramIndex, long p_nodeId)
    {
        m_parameter = p_parameter;
        m_actionIndex = p_actionIndex;
        m_paramIndex = p_paramIndex;
        m_nodeId = p_nodeId;
    }
    
    @Override
    public EsnDataTypes getDataType()
    {
        return m_parameter.getDataType();
    }

    @Override
    public HashMap<Integer, String> getEnumValueMap()
    {
        return m_parameter.getEnumValueMap();
    }

    @Override
    public void setDataType(EsnDataTypes p_dataType)
    {
        m_parameter.setDataType(p_dataType);
    }

    @Override
    public EsnParamValidationTypes getValidationType()
    {
        return m_parameter.getValidationType();
    }

    @Override
    public void setValidationType(EsnParamValidationTypes p_validationType)
    {
        m_parameter.setValidationType(p_validationType);
    }

    @Override
    public short getParameterId()
    {
        return m_parameter.getParameterId();
    }

    @Override
    public String getParameterIdString()
    {
        return m_parameter.getParameterIdString();
    }

    @Override
    public String getName()
    {
        return m_parameter.getName();
    }

    @Override
    public void setName(String p_strName)
    {
        m_parameter.setName(p_strName);
    }

    @Override
    public String getStrValue()
    {
        return m_parameter.getStrValue();
    }

    @Override
    public short getDependentParamId()
    {
        return m_parameter.getDependentParamId();
    }

    @Override
    public void setDependentParamId(short p_dependentParamId)
    {
        m_parameter.setDependentParamId(p_dependentParamId);
    }

    @Override
    public long getMinimumValue()
    {
        return m_parameter.getMinimumValue();
    }

    @Override
    public void setMinimumValue(long p_minimumValue)
    {
        m_parameter.setMinimumValue(p_minimumValue);
    }

    @Override
    public long getMaximumValue()
    {
        return m_parameter.getMaximumValue();
    }

    @Override
    public void setMaximumValue(long p_maximumValue)
    {
        m_parameter.setMaximumValue(p_maximumValue);
    }

    @Override
    public int getMaxStringLength()
    {
        return m_parameter.getMaxStringLength();
    }

    @Override
    public void setMaxStringLength(int p_maxStringLength)
    {
        m_parameter.setMaxStringLength(p_maxStringLength);
    }

    @Override
    public void setEnumMap(HashMap<Integer, String> p_enumMap)
    {
        m_parameter.setEnumMap(p_enumMap);
    }

    @Override
    public boolean getIsSigned()
    {
        return m_parameter.getIsSigned();
    }

    @Override
    public boolean getIsInteger()
    {
        return m_parameter.getIsInteger();
    }

    @Override
    public String getRange()
    {
        return m_parameter.getRange();
    }

    @Override
    public String getDescription()
    {
        return m_parameter.getDescription();
    }

    @Override
    public long getIntValue()
    {
        return m_parameter.getIntValue();
    }

    @Override
    public boolean setValue(String p_value)
    {
        return m_parameter.setValue(p_value);
    }

    @Override
    public boolean getIsString()
    {
        return m_parameter.getIsString();
    }

    @Override
    public Element serialize()
    {
        return m_parameter.serialize();
    }

    @Override
    public boolean isEqualTo(IParameter p_param, boolean p_compareId)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getParameterIndex()
    {
        return m_paramIndex;
    }

    @Override
    public int getFunctionIndex()
    {
        return m_actionIndex;
    }

    @Override
    public long getNodeId()
    {
        return m_nodeId;
    }

    @Override
    public boolean isEqualTo(INodeParameter p_param, boolean p_compareId)
    {
        return false;
    }

    @Override
    public short getActionId()
    {
        return m_parameter.getActionId();
    }

    @Override
    public void setActionId(short p_actionId)
    {
        // TODO: stub
    }

    @Override
    public EsnActionParameterType getParameterType()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setParameterType(EsnActionParameterType p_type)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isEqualTo(IActionParameter p_param, boolean p_compareId)
    {
        return m_parameter.isEqualTo(p_param, p_compareId);
    }
}
