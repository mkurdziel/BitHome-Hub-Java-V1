/**
 * 
 */
package synet.controller.messaging.messages;

import java.util.HashMap;

import synet.controller.Protocol.EsnDataTypes;
import synet.controller.nodes.NodeBase;
import synet.controller.utils.EBitConverter;
import synet.controller.utils.Logger;

/**
 * @author mkurdziel
 *
 */
public class MsgCatalogResponse extends Msg
{
	private static final String MSG_TYPE = "Catalog Response";
	private static final String TAG = "MsgCatalogResponse";
	
	private int m_totalEntries;
	private int m_entryNumber;
	private int m_numParams;
	private EsnDataTypes m_returnType = EsnDataTypes.VOID;
	private HashMap<Integer, EsnDataTypes> m_paramType = 
		new HashMap<Integer, EsnDataTypes>();
	private String m_functionName;
	
	/**
	 * @param p_sourceNode
	 * @param p_destinationNode
	 * @param p_totalEntries
	 * @param p_entryNum
	 * @param p_numParams
	 * @param p_functionName
	 * @param p_paramTypes
	 */
	public MsgCatalogResponse(
	        NodeBase p_sourceNode,
            NodeBase p_destinationNode,
            int p_totalEntries,
            int p_entryNum,
            int p_numParams,
            EsnDataTypes p_returnType,
            String p_functionName,
            HashMap<Integer,EsnDataTypes> p_paramTypes)
	{
	    super(p_sourceNode, p_destinationNode);
	    
	    m_totalEntries = p_totalEntries;
	    m_entryNumber = p_entryNum;
	    m_numParams = p_numParams;
	    m_functionName = p_functionName;
	    m_paramType = p_paramTypes;
	    m_returnType = p_returnType;
	}
	
	/**
	 * Default constructor
	 * 
	 * @param p_sourceNode
	 * @param p_destinationNode
	 * @param p_data
	 * @param p_dataOffset
	 */
	public MsgCatalogResponse(NodeBase p_sourceNode,
			NodeBase p_destinationNode,
			int[] p_data,
			int p_dataOffset)
	{
		super(p_sourceNode, p_destinationNode);
		
		m_totalEntries = p_data[p_dataOffset + 2];
		m_entryNumber = p_data[p_dataOffset + 3];
		m_numParams = p_data[p_dataOffset + 4];
		
		Logger.v(TAG, "entry number: " + m_entryNumber);
		
		if (m_entryNumber != 0)
		{
			m_returnType = EsnDataTypes.get(p_data[p_dataOffset + 5]);
			for (int i=0; i<m_totalEntries; ++i)
			{
				m_paramType.put(i, EsnDataTypes.get(p_data[p_dataOffset + 6 + i]));
			}

			m_functionName = EBitConverter.toString(p_data, p_dataOffset + 6 + m_numParams);
		}
	}
	

	/**
	 * @return the total number of catalog entries
	 */
	public int getTotalEntries()
	{
		return m_totalEntries;
	}

	/**
	 * @return the current entry number
	 */
	public int getEntryNumber()
	{
		return m_entryNumber;
	}

	/**
	 * @return the number of parameters for this entry
	 */
	public int getNumParams()
	{
		return m_numParams;
	}

	/**
	 * @return the return type for this entry
	 */
	public EsnDataTypes getReturnType()
	{
		return m_returnType;
	}

	/**
	 * @return a map of parameter types
	 */
	public HashMap<Integer, EsnDataTypes> getParamType()
	{
		return m_paramType;
	}

	/**
	 * @return the function name
	 */
	public String getFunctionName()
	{
		return m_functionName;
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
		return MsgConstants.SN_API_CATALOG_RESPONSE;
	}
}
