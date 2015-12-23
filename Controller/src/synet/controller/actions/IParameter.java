package synet.controller.actions;

import java.util.HashMap;

import nu.xom.Element;

import synet.controller.Protocol.EsnDataTypes;
import synet.controller.Protocol.EsnParamValidationTypes;

public interface IParameter
{
    /**
     * @return the m_dataType
     */
    public EsnDataTypes getDataType();

    /**
     * @return
     */
    public HashMap<Integer, String> getEnumValueMap();

    /**
     * @param mDataType the m_dataType to set
     */
    public void setDataType(EsnDataTypes p_dataType);

    /**
     * @return the m_validationType
     */
    public EsnParamValidationTypes getValidationType();

    /**
     * @param mValidationType the m_validationType to set
     */
    public void setValidationType(EsnParamValidationTypes p_validationType);

    /**
     * @return the m_parameterId
     */
    public short getParameterId();

    /**
     * @return the Parameter ID as a string
     */
    public String getParameterIdString();

    /**
     * @return the m_strName
     */
    public String getName();

    /**
     * @param mStrName the m_strName to set
     */
    public void setName(String p_strName);

    /**
     * @return the m_strValue
     */
    public String getStrValue();

    /**
     * @return the m_dependentParamId
     */
    public short getDependentParamId();

    /**
     * @param mDependentParamId the m_dependentParamId to set
     */
    public void setDependentParamId(short p_dependentParamId);

    /**
     * @return the m_minimumValue
     */
    public long getMinimumValue();

    /**
     * @param mMinimumValue the m_minimumValue to set
     */
    public void setMinimumValue(long p_minimumValue);

    /**
     * @return the m_maximumValue
     */
    public long getMaximumValue();

    /**
     * @param mMaximumValue the m_maximumValue to set
     */
    public void setMaximumValue(long p_maximumValue);

    /**
     * @return the m_maxStringLength
     */
    public int getMaxStringLength();

    /**
     * @param mMaxStringLength the m_maxStringLength to set
     */
    public void setMaxStringLength(int p_maxStringLength);

    /**
     * Set the enumeration map
     * 
     * @param p_enumMap
     */
    public void setEnumMap(HashMap<Integer, String> p_enumMap);

    /**
     * @return the m_isSigned
     */
    public boolean getIsSigned();

    /**
     * @return true if this parameter is an integer
     */
    public boolean getIsInteger();

    /**
     * @return a string with the range of valid parameter values
     */
    public String getRange();

    /**
     * @return a string describing the parameter
     */
    public String getDescription();

    /**
     * @return the int value or zero if unvalidated
     */
    public long getIntValue();

    /**
     * Set the parameter value
     * 
     * @param p_value
     * @return
     */
    public boolean setValue(String p_value);

    /**
     * @return true if this parameter is a string
     */
    public boolean getIsString();

    /**
     * @return serialized XML of the parameter
     */
    public Element serialize();

    /** 
     * Deep compare this parameter to another parameter
     * 
     * @param p_param
     * @return
     */
    public boolean isEqualTo(IParameter p_param, boolean p_compareId);
}
