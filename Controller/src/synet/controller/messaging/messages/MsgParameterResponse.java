/**
 * 
 */
package synet.controller.messaging.messages;

import java.util.HashMap;

import synet.controller.Protocol.EsnDataTypes;
import synet.controller.Protocol.EsnParamValidationTypes;
import synet.controller.nodes.NodeBase;
import synet.controller.utils.EBitConverter;
import synet.controller.utils.Logger;

/**
 * @author mkurdziel
 *
 */
public class MsgParameterResponse extends Msg
{
	private static final int NOT_SET = -1;
	private static final String MSG_TYPE = "Parameter Response";
	private static final String TAG = "MsgParameterResponse";
	
	private int m_functionId;
	private int m_parameterId;
	private String m_parameterName;
	private EsnDataTypes m_paramDataType;
	private EsnParamValidationTypes m_validationType;
	
	
    private int m_nMaxStringLength;
    private long m_nMinumumValue;
    private long m_nMaximumValue;
    private int m_nValueWidthInBytes;
    // TODO: is this needed?
    private boolean m_bIsSigned;
    private HashMap<Integer, String> m_dctEnumValueByName;

    /**
     * @param p_sourceNode
     * @param p_destinationNode
     * @param p_actionIndex
     * @param p_parameterIndex
     * @param p_parameterName
     * @param p_parameterDataType
     * @param p_validationType
     * @param p_maxStringLen
     * @param p_minValue
     * @param p_maxValue
     * @param p_dctEnumValueByName
     */
    public MsgParameterResponse(
            NodeBase p_sourceNode,
            NodeBase p_destinationNode,
            int p_actionIndex,
            int p_parameterIndex,
            String p_parameterName,
            EsnDataTypes p_parameterDataType,
            EsnParamValidationTypes p_validationType,
            int p_maxStringLen,
            long p_minValue,
            long p_maxValue,
            HashMap<Integer, String> p_dctEnumValueByName)
    {
        super(p_sourceNode, p_destinationNode);
        
        m_functionId = p_actionIndex;
        m_parameterId = p_parameterIndex;
        m_parameterName = p_parameterName;
        m_nMaximumValue = p_maxValue;
        m_nMinumumValue = p_minValue;
        m_paramDataType = p_parameterDataType;
        m_validationType = p_validationType;
        m_nMaxStringLength = p_maxStringLen;
    }
    
	/**
	 * Default constructor
	 * 
	 * @param p_sourceNode
	 * @param p_destinationNode
	 * @param p_data
	 * @param p_dataOffset
	 */
	public MsgParameterResponse(NodeBase p_sourceNode,
			NodeBase p_destinationNode,
			int[] p_data,
			int p_dataOffset)
	{
		super(p_sourceNode, p_destinationNode);
		
		m_dctEnumValueByName = new HashMap<Integer, String>();
		      
		m_nMaxStringLength = NOT_SET;
		m_nMinumumValue = NOT_SET;
		m_nMaxStringLength = NOT_SET;
		m_validationType = EsnParamValidationTypes.UNKNOWN;
		
		p_dataOffset += 2;
		      
		m_functionId = p_data[p_dataOffset++];
		m_parameterId = p_data[p_dataOffset++];

		// get parameter data type
		m_paramDataType = EsnDataTypes.get(p_data[p_dataOffset++]);
		switch (m_paramDataType)
		{
		case BOOL:
			m_nValueWidthInBytes = 1;
			break;
		case BYTE:
			m_nValueWidthInBytes = 1;
			break;
		case WORD:
			m_nValueWidthInBytes = 2;
			break;
		case VOID:
			m_nValueWidthInBytes = 0;
			break;
		case STRING:
			m_nValueWidthInBytes = 0;
			break;
		case DWORD:
			m_nValueWidthInBytes = 4;
			break;
		default:
			Logger.w(TAG, "unrecognized parameter data type: " + m_paramDataType);
			return;
		}

		// get parameter validation type
		m_validationType = EsnParamValidationTypes.get(p_data[p_dataOffset++]);

		// get validation values
		switch (m_validationType)
		{
		case UNSIGNED_FULL:
			// full-range unsigned value
			m_bIsSigned = false;
			break;
		case UNSIGNED_RANGE:
			// load min and max type-width values
			m_nMinumumValue = EBitConverter.loadValueGivenWidth(p_data, p_dataOffset, m_nValueWidthInBytes);
			p_dataOffset += m_nValueWidthInBytes;
			m_nMaximumValue = EBitConverter.loadValueGivenWidth(p_data, p_dataOffset, m_nValueWidthInBytes);
			p_dataOffset += m_nValueWidthInBytes;
			m_bIsSigned = false;
			break;
		case SIGNED_FULL:
			// full-range signed value
			m_bIsSigned = true;
			break;
		case SIGNED_RANGE:
			m_bIsSigned = true;
			// load min and max type-width values
			m_nMinumumValue = EBitConverter.loadValueGivenWidth(p_data, p_dataOffset, m_nValueWidthInBytes);
			p_dataOffset += m_nValueWidthInBytes;
			m_nMaximumValue = EBitConverter.loadValueGivenWidth(p_data, p_dataOffset, m_nValueWidthInBytes);
			p_dataOffset += m_nValueWidthInBytes;
			break;
		case ENUMERATED:
			// load count, then value-name pairs count times
			int nNbrEnumValues = p_data[p_dataOffset++];
			int nEnumValue;
			String strEnumValueName;
			for (int nEntryIdx = 0; nEntryIdx < nNbrEnumValues; nEntryIdx++)
			{
				nEnumValue = EBitConverter.loadValueGivenWidth(p_data, p_dataOffset, m_nValueWidthInBytes);
				p_dataOffset += m_nValueWidthInBytes;
				strEnumValueName = EBitConverter.toString(p_data, p_dataOffset);
				p_dataOffset += strEnumValueName.length()+1;
				m_dctEnumValueByName.put(nEnumValue,strEnumValueName);
			}
			break;
		case MAX_STRING_LEN:
			// load single byte max string length
			m_nMaxStringLength = p_data[p_dataOffset++];
			break;
		}

		m_parameterName = EBitConverter.toString(p_data, p_dataOffset);
	}
	
	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getMsgType()
	 */
	@Override
	public String getMsgType()
	{
		return MSG_TYPE;
	}

	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getAPI()
	 */
	@Override
	public byte getAPI()
	{
		return MsgConstants.SN_API_PARAMETER_RESPONSE;
	}

	/**
	 * @return the data type
	 */
	public EsnDataTypes getDataType()
	{
		return m_paramDataType;
	}
	
	/**
	 * @return the validation type
	 */
	public EsnParamValidationTypes getValidationType()
	{
		return m_validationType;
	}
	
	/**
	 * @return the enumeration map
	 */
	public HashMap<Integer, String> getEnumValues()
	{
		return m_dctEnumValueByName;
	}
	
	/**
	 * @return the function ID
	 */
	public int getFunctionId()
	{
		return m_functionId;
	}

	/**
	 * @return the parameter ID
	 */
	public int getParameterId()
	{
		return m_parameterId;
	}

	/**
	 * @return the parameter name
	 */
	public String getParameterName()
	{
		return m_parameterName;
	}

	/**
	 * @return the max string length
	 */
	public int getMaxStringLength()
	{
		return m_nMaxStringLength;
	}

	/**
	 * @return the minimum value
	 */
	public long getMinumumValue()
	{
		return m_nMinumumValue;
	}

	/**
	 * @return the maximum value
	 */
	public long getMaximumValue()
	{
		return m_nMaximumValue;
	}

	/**
	 * @return if the parameter is signed
	 */
	public boolean getIsSigned()
	{
		return m_bIsSigned;
	}

	public String getInformation()
	{
		StringBuilder sbInfoText = new StringBuilder();
		sbInfoText.append(m_functionId);
		sbInfoText.append("-");
		sbInfoText.append(m_parameterId);
		sbInfoText.append(" ");
		sbInfoText.append(m_parameterName);
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
			sbInfoText.append(String.format(" [%d-%d]", m_nMinumumValue, m_nMaximumValue));
			break;
		case SIGNED_FULL:
			// full-range signed value
			sbInfoText.append("Signed Full");
			break;
		case SIGNED_RANGE:
			sbInfoText.append("Signed Range");
			sbInfoText.append(String.format(" [%d-%d]", m_nMinumumValue, m_nMaximumValue));
			break;
		case ENUMERATED:
			// load count, then value-name pairs count times
			sbInfoText.append("Table [ ");
			for(Integer strEnumName : m_dctEnumValueByName.keySet())
			{
				String nEnumValue = m_dctEnumValueByName.get(strEnumName);
				sbInfoText.append(String.format("%d=%s ", strEnumName, nEnumValue));
			}
			sbInfoText.append("]");
			break;
		case MAX_STRING_LEN:
			// load single byte max string length
			sbInfoText.append(String.format("string:Max %d bytes", m_nMaxStringLength));
			break;
		}
		return sbInfoText.toString();
	}
	
	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getDescription()
	 */
	@Override
	public String getDescription()
	{
	    return String.format("%s actionIndex:%d paramIndex:%d", TAG, m_functionId, m_parameterId);
	}
}
