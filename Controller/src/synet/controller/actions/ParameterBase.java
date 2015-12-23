package synet.controller.actions;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Map.Entry;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

import synet.controller.Protocol;
import synet.controller.Protocol.EsnDataTypes;
import synet.controller.Protocol.EsnParamValidationTypes;
import synet.controller.utils.Logger;
import synet.controller.utils.XmlUtils;

public abstract class ParameterBase implements IParameter {
    private static final String C_STR_XML_PARAM_DEPENDENTID = "dependentParameterId";
    private static final String C_STR_XML_PARAM_ENUMVALUE = "value";
    private static final String C_STR_XML_PARAM_ENUMNAME = "name";
    private static final String C_STR_XML_PARAM_ENUM = "enumerationValue";
    private static final String C_STR_XML_PARAM_ENUMVALUES = "enumerationValues";
    public static final String C_STR_XML_PARAMETER = "parameter";
    private static final String C_STR_XML_PARAM_MAXSTRINGLEN = "maxStringLen";
    private static final String C_STR_XML_PARAM_MAXVALUE = "maxValue";
    private static final String C_STR_XML_PARAM_MINVALUE = "minValue";
    private static final String C_STR_XML_PARAM_VALIDATIONTYPE = "validationType";
    private static final String C_STR_XML_PARAM_DATATYPE = "dataType";
    private static final String C_STR_XML_PARAM_NAME = "name";
    private static final String C_STR_XML_PARAM_ID = "id";

    private static short C_UBYTE_MAX = 255;
    private static short C_UBYTE_MIN = 0;
    private static short C_SBYTE_MAX = 127;
    private static short C_SBYTE_MIN = -128;
    private static int C_UWORD_MAX = 65535;
    private static int C_UWORD_MIN = 0;
    private static int C_SWORD_MAX = 32767;
    private static int C_SWORD_MIN = -32768;
    private static long C_UDWORD_MAX = Long.parseLong("4294967295");
    private static long C_UDWORD_MIN = 0;
    private static long C_SDWORD_MAX = Integer.MAX_VALUE;
    private static long C_SDWORD_MIN = Integer.MIN_VALUE;
    private static String C_UQWORD_MAX = "18446744073709551615"; // TO DO
    private static long C_UQWORD_MIN = 0;
    private static long C_SQWORD_MAX = Long.MAX_VALUE;
    private static long C_SQWORD_MIN = Long.MIN_VALUE;

    private static final String TAG = "ParameterBase";

    private short m_parameterId;
    private String m_strName = "";
    private String m_strValue = "";
    private long m_intValue;
    private short m_dependentParamId;
    private HashMap<Integer, String> m_enumByValueMap = null;
    private EsnDataTypes m_dataType;
    private EsnParamValidationTypes m_validationType;
    private long m_minimumValue = 0;
    private long m_maximumValue = 0;
    private int m_maxStringLength = 0;

    public ParameterBase (
            short p_parameterId,
            String p_strName,
            EsnDataTypes p_dataType,
            EsnParamValidationTypes p_paramValidataionType,
            long p_minimumValue,
            long p_maximumValue,
            int p_maxStringLength,
            HashMap<Integer, String> p_dctEnumValueByName)
    {
        m_parameterId = p_parameterId;
        setName(p_strName);
        setDataType(p_dataType);
        setValidationType(p_paramValidataionType);
        setMinimumValue(p_minimumValue);
        setMaximumValue(p_maximumValue);
        setMaxStringLength(p_maxStringLength);
        m_enumByValueMap = p_dctEnumValueByName;

        // prevent us from accessing a null map
        if (m_enumByValueMap == null)
        {
            m_enumByValueMap = new HashMap<Integer, String>();
        }

        Logger.v(TAG, String.format("Constructor - ID:%s,Name:%s,DataType:%s", getParameterIdString(), getName(), getDataType()));
        Logger.v(TAG, String.format("Constructor+- ValType:%s,Min:%x,Max:%x,StrLen:%d", getValidationType(), getMaximumValue(), getMaximumValue(), getMaxStringLength()));
        Iterator<Entry<Integer, String>> it = m_enumByValueMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, String> pairs = it.next();
            // Add the two attributes
            Logger.v(TAG, String.format("Constructor+- Enum key:%d,val:%s", pairs.getKey(), pairs.getValue()));
        }
    }

    /**
     * Deserialization constructor
     * 
     * @param p_xml
     */
    public ParameterBase(Element p_xml)
    {
        this.deserialize(p_xml);
    }

    /**
     * @return the m_dataType
     */
    public EsnDataTypes getDataType() {
        return m_dataType;
    }
    
    /**
     * @return
     */
    public HashMap<Integer, String> getEnumValueMap()
    {
        return m_enumByValueMap;
    }

    /**
     * @param mDataType the m_dataType to set
     */
    public void setDataType(EsnDataTypes p_dataType) {
        m_dataType = p_dataType;
        // If it's a string, validation is string length
        if (p_dataType == EsnDataTypes.STRING)
        {
            setValidationType(EsnParamValidationTypes.MAX_STRING_LEN); 
        }
        else if(p_dataType == EsnDataTypes.BOOL)
        {
            setValidationType(EsnParamValidationTypes.BOOL);
        }
    }

    /**
     * @return the m_validationType
     */
    public EsnParamValidationTypes getValidationType() {
        return m_validationType;
    }

    /**
     * @param mValidationType the m_validationType to set
     */
    public void setValidationType(EsnParamValidationTypes p_validationType) {
        m_validationType = p_validationType;
    }

    /**
     * @return the m_parameterId
     */
    public short getParameterId() {
        return m_parameterId;
    }

    /**
     * @return the Parameter ID as a string
     */
    public String getParameterIdString()
    {
        return String.format("0x%x", m_parameterId);
    }

    /**
     * @return the m_strName
     */
    public String getName() {
        return m_strName;
    }

    /**
     * @param mStrName the m_strName to set
     */
    public void setName(String p_strName) {
        m_strName = p_strName;
    }

    /**
     * @return the m_strValue
     */
    public String getStrValue() {
        return m_strValue;
    }

    /**
     * @return the m_dependentParamId
     */
    public short getDependentParamId() {
        return m_dependentParamId;
    }

    /**
     * @param mDependentParamId the m_dependentParamId to set
     */
    public void setDependentParamId(short p_dependentParamId) {
        m_dependentParamId = p_dependentParamId;
    }

    /**
     * @return the m_minimumValue
     */
    public long getMinimumValue() {
        return m_minimumValue;
    }

    /**
     * @param mMinimumValue the m_minimumValue to set
     */
    public void setMinimumValue(long p_minimumValue) {
        m_minimumValue = p_minimumValue;
    }

    /**
     * @return the m_maximumValue
     */
    public long getMaximumValue() {
        return m_maximumValue;
    }

    /**
     * @param mMaximumValue the m_maximumValue to set
     */
    public void setMaximumValue(long p_maximumValue) {
        m_maximumValue = p_maximumValue;
    }

    /**
     * @return the m_maxStringLength
     */
    public int getMaxStringLength() {
        return m_maxStringLength;
    }

    /**
     * @param mMaxStringLength the m_maxStringLength to set
     */
    public void setMaxStringLength(int p_maxStringLength) {
        if (p_maxStringLength < 0)
        {
            m_maxStringLength = 0;
        }
        else
        {
            m_maxStringLength = p_maxStringLength;
        }
    }

    /**
     * Set the enumeration map
     * 
     * @param p_enumMap
     */
    public void setEnumMap(HashMap<Integer, String> p_enumMap)
    {
        m_enumByValueMap = p_enumMap;
    }

    /**
     * @return the m_isSigned
     */
    public boolean getIsSigned() {
        switch(m_validationType)
        {
            case SIGNED_FULL:
            case SIGNED_RANGE:
                return true;
        }
        return false;
    }

    /**
     * @return true if this parameter is an integer
     */
    public boolean getIsInteger()
    {
        boolean bIntStatus = true;
        switch (m_validationType)
        {
            case DATE_TIME:
            case MAX_STRING_LEN:
            case UNKNOWN:
                bIntStatus = false; // no, is quadword (64-bits) date-value
                break;
        }
        return bIntStatus;
    }

    /**
     * @return a string with the range of valid parameter values
     */
    public String getRange()
    {
        String retVal = "unknown";
        switch (m_validationType)
        {
            case BOOL:
                retVal = "True or False";
                break;
            case ENUMERATED:
                retVal = "Enumeration";
                break;
            case MAX_STRING_LEN:
                retVal = String.format("%d-character string", m_maxStringLength);
                break;
            case SIGNED_FULL:
                String min = "NA";
                String max = "NA";
                switch (m_dataType)
                {
                    case BYTE:
                        min = C_SBYTE_MIN + "";
                        max = C_SBYTE_MAX + "";
                        break;
                    case WORD:
                        min = C_SWORD_MIN + "";
                        max = C_SWORD_MAX + "";
                        break;
                    case DWORD:
                        min = C_SDWORD_MIN + "";
                        max = C_SDWORD_MAX + "";
                        break;
                    case QWORD:
                        min = C_SQWORD_MIN + "";
                        max = C_SQWORD_MAX + "";
                        break;
                    default:
                        Logger.w(TAG, "Parameter.Range - something went wrong");
                        break;
                }
                retVal = String.format("%s - %s", min, max);
                break;
            case UNSIGNED_FULL:
                min = "NA";
                max = "NA";
                switch (m_dataType)
                {
                    case BYTE:
                        min = C_UBYTE_MIN + "";
                        max = C_UBYTE_MAX + "";
                        break;
                    case WORD:
                        min = C_UWORD_MIN + "";
                        max = C_UWORD_MAX + "";
                        break;
                    case DWORD:
                        min = C_UDWORD_MIN + "";
                        max = C_UDWORD_MAX + "";
                        break;
                    case QWORD:
                        min = C_UQWORD_MIN + "";
                        max = C_UQWORD_MAX + "";
                        break;
                    default:
                        Logger.w(TAG, "Parameter.Range - something went wrong");
                        break;
                }
                retVal = String.format("%s - %s", min, max);
                break;
            case UNKNOWN:
                break;
            case SIGNED_RANGE:
            case UNSIGNED_RANGE:
                retVal = String.format("%d - %d", m_minimumValue, m_maximumValue);
                break;
        }
        return retVal;
    }

    /**
     * @return a string describing the parameter
     */
    public String getDescription()
    {
        StringBuilder sbInfoText = new StringBuilder();
        sbInfoText.append(m_strName);
        sbInfoText.append(": ");
        sbInfoText.append(m_dataType);
        sbInfoText.append(": ");

        switch (m_validationType)
        {
            case UNSIGNED_FULL:
                // full-range unsigned value
                sbInfoText.append("Unsigned Full");
                break;
            case UNSIGNED_RANGE:
                // load min and max type-width values
                sbInfoText.append("Unsigned Range");
                sbInfoText.append(String.format(" [%d-%d]", m_minimumValue, m_maximumValue));
                break;
            case SIGNED_FULL:
                // full-range signed value
                sbInfoText.append("Signed Full");
                break;
            case SIGNED_RANGE:
                sbInfoText.append("Signed Range");
                sbInfoText.append(String.format(" [%d-%d]", m_minimumValue, m_maximumValue));
                break;
            case ENUMERATED:
                // load count, then value-name pairs count times
                sbInfoText.append("Table [ ");
                for(Integer strEnumName : m_enumByValueMap.keySet())
                {
                    String nEnumValue = m_enumByValueMap.get(strEnumName);
                    sbInfoText.append(String.format("%d=%s ", strEnumName, nEnumValue));
                }
                sbInfoText.append("]");
                break;
            case MAX_STRING_LEN:
                // load single byte max string length
                sbInfoText.append(String.format("string:Max %d bytes", m_maxStringLength));
                break;
        }
        return sbInfoText.toString();
    }

    /**
     * @return the int value or zero if unvalidated
     */
    public long getIntValue()
    {
        return m_intValue;
    }

    /**
     * Set the parameter value
     * 
     * @param p_value
     * @return
     */
    public boolean setValue(String p_value)
    {
        if (validateValue(p_value))
        {
            m_strValue = p_value;
            return true;
        }
        else
        {
            Logger.w(TAG, "invalid value");
        }
        return false;
    }

    /**
     * @return true if this parameter is a string
     */
    public boolean getIsString()
    {
        return (m_validationType == EsnParamValidationTypes.MAX_STRING_LEN ? true : false);
    }

    /**
     * Validate a value against the parameter
     * @param p_value
     * @return
     */
    private boolean validateValue(String p_value)
    {
        boolean bRetVal = getIsString()
        ? validateString(p_value)
                : validateInt(p_value);

        return bRetVal;

    }

    /**
     * Validate a string parameter
     * 
     * @param p_value
     * @return
     */
    private boolean validateString(String p_value)
    {
        return (p_value.length() <= m_maxStringLength);
    }

    /**
     * Validate an int parameter
     * 
     * @param p_value
     * @return
     */
    private boolean validateInt(String p_value)
    {
        
        boolean retVal = true;
        long sq = 0;

        switch (m_validationType)
        {
            case BOOL:
                if (m_dataType == EsnDataTypes.BOOL)
                {
                    retVal = p_value.equals("0") | p_value.equals("1");
                    if (retVal);
                    {
                        sq = Integer.parseInt(p_value);
                    }
                    
                }
                break;
            case ENUMERATED:
                try {
                    sq = Integer.parseInt(p_value);
                    retVal = (m_enumByValueMap.get(Integer.parseInt(p_value)) != null);
                }
                catch (NumberFormatException e)
                {
                    retVal = false;
                }
                break;
            case SIGNED_FULL:
            case SIGNED_RANGE:
                try{
                    switch (m_dataType)
                    {
                        case BYTE:
                            byte sb;
                            sb = Byte.valueOf(p_value);
                            sq = sb;
                            break;
                        case WORD:
                            short sw;
                            sw = Short.valueOf(p_value);
                            sq = sw;
                            break;
                        case DWORD:
                            int sd;
                            sd = Integer.valueOf(p_value);
                            sq = sd;
                            break;
                        case QWORD:
                            sq = Long.valueOf(p_value);
                            break;
                    }
                }
                catch (NumberFormatException e)
                {
                    retVal = false;
                }

                // Do all range checking in one place
                if (m_validationType == EsnParamValidationTypes.SIGNED_RANGE && retVal)
                {
                    retVal = sq <= m_maximumValue && sq >= m_minimumValue;
                }
                break;
            case UNSIGNED_FULL:
            case UNSIGNED_RANGE:
                try{
                    switch (m_dataType)
                    {
                        case BYTE:
                            sq = Long.parseLong(p_value);
                            retVal = sq >= 0 && sq <=255;
                            break;
                        case WORD:
                            sq = Long.parseLong(p_value);
                            retVal = sq >= 0 && sq <=65535;
                            break;
                        case DWORD:
                            sq = Long.parseLong(p_value);
                            retVal = sq >= 0 && sq <= Long.parseLong("4294967295");
                            break;
                        case QWORD:
                            BigInteger bigInt = new BigInteger(p_value);
                            if (bigInt.compareTo(new BigInteger("0")) < 0 || bigInt.bitLength() > 64)
                            {
                                retVal = false;
                            }
                            else
                            {
                                // If we're within the signed range, convert to long
                                if (bigInt.bitLength() < 64)
                                {
                                    sq = bigInt.longValue();
                                }
                                else
                                {
                                    // Clear the sign bit and or the rest
                                    bigInt.clearBit(64);
                                    sq |= 0x8000000000000000L;
                                    sq |= bigInt.longValue();
                                }
                            }
                            break;
                    }
                }
                catch(NumberFormatException e)
                {
                    retVal = false;
                }

                // Do all range checking in one place
                if (m_validationType == EsnParamValidationTypes.UNSIGNED_RANGE && retVal)
                {
                    retVal = sq <= m_maximumValue && sq >= m_minimumValue;
                }
                break;
            case DATE_TIME:
                switch (m_dataType)
                {
                    case QWORD:
                        //				DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
                        try
                        {
                            //DateTime dt = fmt.parseDateTime(p_value);
                            retVal = false;
                        }
                        catch (NumberFormatException e)
                        {
                            retVal = false;
                        }
                        break;
                    default:
                        retVal = false; // any other data types for DATE_TIME fail validation
                        break;
                }
                break;
            case UNKNOWN:
                // If unknown, we don't know how to validate so return true.
                retVal = true;
                break;
        }

        // Cache this int value
        if (retVal)
        {
            m_intValue = sq;
        }
        return retVal;
    }

    /**
     * @return serialized XML of the parameter
     */
    public Element serialize()
    {
        Element paramElement = new Element(C_STR_XML_PARAMETER);

        Attribute paramIdAttribute = new Attribute(C_STR_XML_PARAM_ID, getParameterIdString());
        Attribute paramNameAttribute = new Attribute(C_STR_XML_PARAM_NAME, getName());
        Attribute paramDataTypeAttribute = new Attribute(C_STR_XML_PARAM_DATATYPE, String.valueOf(getDataType()));
        Attribute paramValTypeAttribute = new Attribute(C_STR_XML_PARAM_VALIDATIONTYPE, String.valueOf(getValidationType()));
        Attribute paramMinValueAttribute = new Attribute(C_STR_XML_PARAM_MINVALUE, hexString(getMinimumValue()));
        Attribute paramMaxValueAttribute = new Attribute(C_STR_XML_PARAM_MAXVALUE, hexString(getMaximumValue()));
        Attribute paramMaxStringLenAttribute = new Attribute(C_STR_XML_PARAM_MAXSTRINGLEN, hexString(getMaxStringLength()));
        Attribute paramDependentParamIdAttribute = new Attribute(C_STR_XML_PARAM_DEPENDENTID, hexString(getDependentParamId()));

        paramElement.addAttribute(paramIdAttribute);
        paramElement.addAttribute(paramNameAttribute);
        paramElement.addAttribute(paramDataTypeAttribute);
        paramElement.addAttribute(paramValTypeAttribute);
        paramElement.addAttribute(paramMinValueAttribute);
        paramElement.addAttribute(paramMaxValueAttribute);
        paramElement.addAttribute(paramMaxStringLenAttribute);
        paramElement.addAttribute(paramDependentParamIdAttribute);

        // If enumerated, write out the enumeration values
        if (m_validationType == EsnParamValidationTypes.ENUMERATED)
        {
            Element enumValuesElement = new Element(C_STR_XML_PARAM_ENUMVALUES);

            Iterator<Entry<Integer,String>> it = m_enumByValueMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, String> pairs = it.next();
                Element enumElement = new Element(C_STR_XML_PARAM_ENUM);
                // Add the two attributes
                enumElement.addAttribute(new Attribute(C_STR_XML_PARAM_ENUMNAME, pairs.getValue()));
                enumElement.addAttribute(new Attribute(C_STR_XML_PARAM_ENUMVALUE, pairs.getKey().toString()));
                // Add the child to the parent
                enumValuesElement.appendChild(enumElement);
            }

            paramElement.appendChild(enumValuesElement);
        }

        return paramElement;
    }

    /**
     * @param p_value
     * @return
     */
    private String hexString(long p_value)
    {
        return String.format("0x%x", p_value);
    }
    
    /**
     * @param p_value
     * @return
     */
    private String hexString(short p_value)
    {
        return String.format("0x%x", p_value);
    }

    /**
     * @param p_value
     * @return
     */
    private String hexString(int p_value)
    {
        return String.format("0x%x", p_value);
    }
    
    /**
     * Deserialization method
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

        if (p_xml.getLocalName().compareTo(C_STR_XML_PARAMETER) == 0)
        {
            m_parameterId = XmlUtils.getXmlAttributeShort(p_xml, C_STR_XML_PARAM_ID);
            if (m_parameterId != 0)
            {
                m_strName = XmlUtils.getXmlAttributeString(p_xml, C_STR_XML_PARAM_NAME);
                m_dataType = XmlUtils.getXmlAttributeDataType(p_xml, C_STR_XML_PARAM_DATATYPE);
                m_validationType = EsnParamValidationTypes.valueOf(XmlUtils.getXmlAttributeString(p_xml, C_STR_XML_PARAM_VALIDATIONTYPE));
                m_minimumValue = XmlUtils.getXmlAttributeLong(p_xml, C_STR_XML_PARAM_MINVALUE);
                m_maximumValue = XmlUtils.getXmlAttributeLong(p_xml, C_STR_XML_PARAM_MAXVALUE);
                m_maxStringLength = XmlUtils.getXmlAttributeInteger(p_xml, C_STR_XML_PARAM_MAXSTRINGLEN);
                m_dependentParamId = XmlUtils.getXmlAttributeShort(p_xml, C_STR_XML_PARAM_DEPENDENTID, m_dependentParamId);

                // If it is an enumeration, parse the enumeration values
                if (m_validationType == EsnParamValidationTypes.ENUMERATED)
                {
                    m_enumByValueMap = new HashMap<Integer, String>();

                    Element enumValuesElement = p_xml.getFirstChildElement(C_STR_XML_PARAM_ENUMVALUES);
                    if (enumValuesElement != null)
                    {
                        Elements enumElements = enumValuesElement.getChildElements(C_STR_XML_PARAM_ENUM);
                        for (int i=0; i<enumElements.size(); ++i)
                        {
                            Element enumElement = enumElements.get(i);
                            Attribute enumNameAttr = enumElement.getAttribute(C_STR_XML_PARAM_ENUMNAME);
                            Attribute enumValueAttr = enumElement.getAttribute(C_STR_XML_PARAM_ENUMVALUE);

                            if (enumNameAttr != null && enumValueAttr != null)
                            {
                                try
                                {
                                    m_enumByValueMap.put(Integer.valueOf(enumValueAttr.getValue()), enumNameAttr.getValue());
                                }
                                catch (NumberFormatException e)
                                {
                                    Logger.w(TAG, "Number format exception parsing enum value", e);
                                }
                            }
                            else
                            {
                                Logger.w(TAG, "enum value deserialized improperly");
                            }
                        }
                    }
                }
            }
            else
            {
                Logger.w(TAG, "deserialization failed. Invalid ID");
                return false;
            }
        }
        else
        {
            Logger.w(TAG, "deserialization failed. Incorrect root node");
        }
        return false;
    }

    /** 
     * Deep compare this parameter to another parameter
     * 
     * @param p_param
     * @return
     */
    public boolean isEqualTo(ParameterBase p_param, boolean p_compareId)
    {
        boolean retVal = true;
        
        if (p_compareId)
        {
            retVal &= getParameterId() == p_param.getParameterId();
        }
        
        retVal &= getDataType() == p_param.getDataType();
        retVal &= getDependentParamId() == p_param.getDependentParamId();
        retVal &= getDescription().equals(p_param.getDescription());
        retVal &= getIntValue() == p_param.getIntValue();
        retVal &= getStrValue().equals(p_param.getStrValue());
        retVal &= getIsInteger() == p_param.getIsInteger();
        retVal &= getIsSigned() == p_param.getIsSigned();
        retVal &= getIsString() == p_param.getIsString();
        retVal &= getMaximumValue() == p_param.getMaximumValue();
        retVal &= getMaxStringLength() == p_param.getMaxStringLength();
        retVal &= getMinimumValue() == p_param.getMinimumValue();
        retVal &= getName().equals(p_param.getName());
        retVal &= getRange().equals(p_param.getRange());
        retVal &= getValidationType() == p_param.getValidationType();
        
        return retVal;
    }
    
    /* (non-Javadoc)
     * @see synet.controller.actions.IParameter#isEqualTo(synet.controller.actions.IParameter, boolean)
     */
    public boolean isEqualTo(IParameter p_param, boolean p_compareId)
    {
        if (p_param instanceof ParameterBase)
        {
            return isEqualTo((ParameterBase)p_param, p_compareId);
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return getDescription();
    }
}
