/**
 * 
 */
package synet.controller.nodes;

import java.util.HashMap;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import synet.controller.actions.INodeAction;
import synet.controller.actions.INodeParameter;
import synet.controller.actions.NodeAction;
import synet.controller.actions.NodeParameter;
import synet.controller.utils.Logger;
import synet.controller.utils.Pair;
import synet.controller.utils.SysUtils;
import synet.controller.utils.XmlUtils;

/**
 * Base class for all devices in the system
 * 
 * @author mkurdziel
 */
public abstract class NodeBase
{
	private static final String TAG = "Node";
	private static final String UNKNOWN_NAME = "Unknown Node";
	
	public static final String C_STR_NODELIST_REVISION = "revision";
	public static final String C_STR_NODELIST_PROFILE = "profile";
	public static final String C_STR_NODELIST_MANUFACTURER = "manufacturer";
	public static final String C_STR_NODELIST_SNID = "synetId";
	public static final String C_STR_NODELIST_NAME = "name";
	public static final String C_STR_NODELIST_LASTSEEN = "lastSeen";
	public static final String C_STR_NODELIST_TYPE = "type";
	public static final String C_STR_NODELIST_STATUS = "status";
	public static final String C_STR_NODELIST_ID = "id";
	public static final String C_STR_NODELIST_NODE = "node";
	
	/**
	 * Enumeration of node status
	 */
	public enum EsnStatusEnum
	{
		// This device has an unknown state and has not yet reported active
		UNKNOWN,
		// A status active message has been received and has been seen in the
		// last two minutes
		ACTIVE,
		// This device has been seen in the last hour
		RECENT,
		// This device has not been seen in the last hour
		DEAD
	}

	/**
	 * Enumeration of the investigation status of this node
	 */
	public enum EsnInvestigationStatusEnum
	{
		// No attempts have been made yet to learn about this node
		UNKNOWN,
		// The basic node information has been received
		INFO,
		// Catalog functions are currently being investigated
		FUNCTION,
		// Function parameters are currently being investigated
		PARAMETER,
		// A full investigation is completed
		COMPLETED,
		// A timeout occured when trying to investigate
		TIMEOUT
	}

	/**
	 * Member Variables
	 */
	private DateTime m_dateLastSeen; // The last time this node was seen
	private String m_name = UNKNOWN_NAME;
	private boolean m_isUnknown; // True if no active messages have been received
	private long m_nodeID; // The node ID

	private EsnInvestigationStatusEnum m_investigationStatus;
	private DateTime m_timeNextInvestigation;
	private int m_numInvestigationRetries;

	private boolean m_isBeingInvestigated = false;
	private boolean m_hasFullCatalog = false;
	private boolean m_hasFullParameters = false;

	private int m_synetID;
	private int m_manufacturerID;
	private int m_profile;
	private int m_revision;

	private HashMap<Integer, INodeAction> m_catalog = new HashMap<Integer, INodeAction>();
	private int m_numTotalFunctions = 0;

	/**
	 * Constructors
	 */

	/**
	 * Default constructor
	 */
	public NodeBase()
	{
		Logger.v(TAG, "constructor()");

		reset();
	}
	
	/**
	 * Deserialization constructor
	 * 
	 * @param p_xml
	 */
	public NodeBase(Element p_xml)
	{
		this(p_xml, false);
	}
	
	/**
     * Deserialization constructor
     * 
     * @param p_xml
     */
    public NodeBase(Element p_xml, boolean p_deserializeActions)
    {
        this();
        Logger.v(TAG, "constructor(XML)");
        deserialize(p_xml);
        
        if (p_deserializeActions)
        {
            deserializeActions(p_xml);
        }
        
    }

	/**
	 * Address constructor
	 * 
	 * @param p_nodeID
	 */
	public NodeBase(long p_nodeID)
	{
		this();

		Logger.v(TAG, "nodeID:" + String.format("0x%x", p_nodeID));

		m_nodeID = p_nodeID;
	}

	/**
	 * Clear out any catalog entries
	 */
	public void clearCatalog()
	{
		for(INodeAction function : m_catalog.values())
		{
			function.clearParameters();
		}

		m_catalog.clear();
		m_numTotalFunctions = -1;
	}

	/**
	 * @param p_numTotalFunctions
	 */
	public void setNumTotalFunctions(int p_numTotalFunctions)
	{
		Logger.v(TAG, getDescString() + " setting num total functions to " + p_numTotalFunctions);
		
		m_numTotalFunctions = p_numTotalFunctions;
	}

	/**
	 * @return the total number of functions or -1 if unknown
	 */
	public int getNumTotalFunctions()
	{
		return m_numTotalFunctions;
	}
	
	/**
	 * @return the size of the action catalog
	 */
	public int getActionCount()
	{
	    return m_catalog.size();
	}

	/**
	 * @param p_functionEntryNum
	 * @return the NodeFunction for the function ID or null if not found
	 */
	public INodeAction getNodeAction(int p_functionEntryNum)
	{
		return m_catalog.get(p_functionEntryNum);
	}

	/**
	 * @param p_functionEntryNum
	 * @param p_paramEntryNum
	 * @return the NodeParameter for the function ID and parameter ID
	 */
	public INodeParameter getParameter(int p_functionEntryNum, int p_paramEntryNum)
	{
		INodeParameter parameter = null;
		INodeAction function = m_catalog.get(p_functionEntryNum);
		if (function != null)
		{
			parameter = function.getParameter(p_paramEntryNum);
		}
		return parameter;
	}

	/**
	 * Set the parameter for the function
	 * 
	 * @param p_functionEntryNum
	 * @param p_paramEntryNum
	 * @param p_parameter
	 * @return
	 */
	public boolean setParameter(int p_functionEntryNum, int p_parameterEntryNum, NodeParameter p_parameter)
	{
		Logger.w(TAG, "adding parameter " + p_parameterEntryNum + " to function " + p_functionEntryNum + " to " + getDescString());

		INodeAction action = m_catalog.get(p_functionEntryNum);

		if(action != null)
		{
			action.addParameter(p_parameter);
			return true;
		}
		return false;
	}

	/*
	 * Accessors
	 */

	/**
	 * @return the m_timeNextInvestigation
	 */
	public DateTime getTimeNextInvestigation() {
		return m_timeNextInvestigation;
	}

	/**
	 * @param mTimeNextInvestigation the m_timeNextInvestigation to set
	 */
	public void setTimeNextInvestigation(DateTime p_datetime) {
		m_timeNextInvestigation = p_datetime;
	}

	/**
	 * @return the m_numInvestigationRetries
	 */
	public int getNumInvestigationRetries() {
		return m_numInvestigationRetries;
	}

	/**
	 * @param mNumInvestigationRetries the m_numInvestigationRetries to set
	 */
	public void setNumInvestigationRetries(int m_numRetries) {
		m_numInvestigationRetries = m_numRetries;
	}

	/**
	 * @return the investigation status
	 */
	public EsnInvestigationStatusEnum getInvestigationStatus()
	{
		return m_investigationStatus;
	}

	/**
	 * Set the investigation status
	 * 
	 * @param p_status
	 */
	public void setInvestigationStatus(EsnInvestigationStatusEnum p_status)
	{
		Logger.v(TAG, "setting investigation status for " + getDescString() + " to " + p_status);

		m_investigationStatus = p_status;
	}

	/**
	 * @return true if this node is being investigated
	 */
	public boolean getIsBeingInvestigated()
	{
		return m_isBeingInvestigated;
	}

	/**
	 * Set whether this node is being investigated
	 * 
	 * @param m_isBeingInvestigated
	 */
	public void setIsBeingInvestigated(boolean m_isBeingInvestigated)
	{
		this.m_isBeingInvestigated = m_isBeingInvestigated;
	}

	/**
	 * @return true if this node has a full catalog
	 */
	public boolean getHasFullCatalog()
	{
		return m_hasFullCatalog;
	}

	/**
	 * Set whether this node has a full catalog
	 * 
	 * @param m_hasFullCatalog
	 */
	public void setHasFullCatalog(boolean m_hasFullCatalog)
	{
		this.m_hasFullCatalog = m_hasFullCatalog;
	}

	/**
	 * @return true if this node has full parameters
	 */
	public boolean getHasFullParameters()
	{
		return m_hasFullParameters;
	}

	/**
	 * Set whether this node has full parameters
	 * 
	 * @param m_hasFullParameters
	 */
	public void setHasFullParameters(boolean m_hasFullParameters)
	{
		this.m_hasFullParameters = m_hasFullParameters;
	}

	/**
	 * Set the last seen time of the node
	 * 
	 * @param p_lastSeen
	 */
	public void setLastSeen(DateTime p_lastSeen)
	{
		m_dateLastSeen = p_lastSeen;
		m_isUnknown = false;
	}

	/**
	 * @return the last seen DateTime
	 */
	public DateTime getLastSeen()
	{
		return m_dateLastSeen;
	}

	/**
	 * @return The name of the device
	 */
	public String getName()
	{
		return m_name;
	}

	/**
	 * Set the name of the device
	 * 
	 * @param p_name
	 */
	public void setName(String p_name)
	{
		m_name = p_name;
		// Notify any listeners
	}

	/**
	 * @return the status of the device
	 */
	public EsnStatusEnum getNodeStatus()
	{
		if (m_isUnknown)
		{
			return EsnStatusEnum.UNKNOWN;
		}

		// Get the current datetime
		Duration activeTime = new Duration((new DateTime()).getMillis() - m_dateLastSeen.getMillis());

		// If the active time is less than five minutes, we are active
		if (activeTime.getStandardMinutes() < 5)
		{
			return EsnStatusEnum.ACTIVE;
		}

		if (activeTime.getStandardHours() < 1)
		{
			return EsnStatusEnum.RECENT;
		}
		return EsnStatusEnum.DEAD;
	}
	
	/**
	 * @return the node status as a string
	 */
	public String getNodeStatusString()
	{
		return String.valueOf(getNodeStatus());
	}

	/**
	 * @return The 64 bit node ID
	 */
	public final long getNodeId()
	{
		return m_nodeID;
	}
	
	/**
	 * @return a string representation of the node ID
	 */
	public final String getNodeIdString()
	{
		return String.format("0x%x", m_nodeID);
	}
	
	/**
	 * @return a string representation of the node ID
	 */
	public final String getNodeIdStringNoX()
	{
		return String.format("%x", m_nodeID);
	}

	/**
	 * Set the node ID. Should be unique among nodes
	 * 
	 * @param p_nodeID
	 */
	public final void setNodeID(long p_nodeID)
	{
		m_nodeID = p_nodeID;
	}

	/**
	 * @return the SyNet ID
	 */
	public int getSynetId()
	{
		return m_synetID;
	}
	
	/**
	 * @return the SyNet ID as a hex string
	 */
	public String getSynetIdString()
	{
		return String.format("0x%x", m_synetID);
	}

	/**
	 * Set the SyNet ID
	 * 
	 * @param m_synetID
	 */
	public void setSynetID(int m_synetID)
	{
		this.m_synetID = m_synetID;
	}

	/**
	 * @return the manufacturer ID
	 */
	public int getManufacturerId()
	{
		return m_manufacturerID;
	}
	
	/**
	 * @return the manufacturer ID as a hex string
	 */
	public String getManufacturerIdString()
	{
		return String.format("0x%x", m_manufacturerID);
	}

	/**
	 * Set the manufacturer ID
	 * 
	 * @param m_manufacturerID
	 */
	public void setManufacturerId(int m_manufacturerID)
	{
		this.m_manufacturerID = m_manufacturerID;
	}

	/**
	 * @return the profile
	 */
	public int getProfile()
	{
		return m_profile;
	}

	/**
	 * @return the profile as a string
	 */
	public String getprofileString() {
		return String.format("0x%x", m_profile);
	}
	
	/**
	 * Set the profile
	 * 
	 * @param m_profile
	 */
	public void setProfile(int m_profile)
	{
		this.m_profile = m_profile;
	}

	/**
	 * @return the revision
	 */
	public int getRevision()
	{
		return m_revision;
	}
	
	/**
	 * @return the revision as a hex string
	 */
	public String getRevisionString()
	{
		return String.format("0x%x", m_revision);
	}

	/**
	 * Set the revision
	 * 
	 * @param m_revision
	 */
	public void setRevision(int m_revision)
	{
		this.m_revision = m_revision;
	}

	/**
	 * Set whether this node is unknown
	 * 
	 * @param p_isUnknown
	 */
	public void setIsUnknown(boolean p_isUnknown)
	{
		m_isUnknown = p_isUnknown;
	}

	/**
	 * Get if this node is unknown
	 * 
	 * @return
	 */
	public boolean getIsUnknown()
	{
		return m_isUnknown;
	}

	/**
	 * @return a string description of the node
	 */
	public String getDescString()
	{
		if (m_name.equals(UNKNOWN_NAME))
		{
			return String.format("0x%x", m_nodeID);
		}
		return m_name;
	}

	/**
	 * @return the next function that needs to be investigated
	 */
	public int getNextUnknownNodeAction()
	{
		for (int i=1; i <= m_numTotalFunctions; ++i)
		{
			if (m_catalog.get((i)) == null)
			{
				return i;
			}
		}
		return 0;
	}

	/**
	 * @return the next function with an unknown parameter
	 */
	public Pair<Integer> getNextUnknownParameter()
	{
		INodeAction action;
		int paramNum;
		
		for (int i=1; i <= m_numTotalFunctions; ++i)
		{
			if ((action = m_catalog.get((i))) != null)
			{
				if ((paramNum = action.getNextUnknownParameter())!= 0)
				{
					return new Pair<Integer>(i, paramNum);
				}
			}
			else
			{
				Logger.e(TAG, "trying to access null node action");
			}
		}
		return null;
	}
	
	/**
	 * @return the next function with an unknown parameter for a function
	 */
	public int getNextUnknownParameter(int p_functionEntryNum)
	{
		INodeAction  action = m_catalog.get(p_functionEntryNum);
		
		if (action != null)
		{
			return action.getNextUnknownParameter();
		}
		else
		{
			Logger.e(TAG, getDescString() + " asking unknown parameter for null action index " + p_functionEntryNum);
		}
		
		return 0;
	}

	/**
	 * @return the function catalog for the node
	 */
	public INodeAction[] getNodeActions()
	{
		return m_catalog.values().toArray(new INodeAction[m_catalog.size()]);
	}

	/**
	 * Set the node action for the entry number
	 * 
	 * @param entryNumber
	 * @param action
	 */
	public INodeAction setNodeAction(int p_entryNumber, INodeAction p_action)
	{
	    INodeAction replacedAction = null;
		Logger.i(TAG, "adding node action " + p_entryNumber + " to " + getDescString());

		if (m_catalog.containsKey(p_entryNumber))
		{
			Logger.w(TAG, "replacing function " + p_entryNumber);
			replacedAction = m_catalog.get(p_entryNumber);
		}
		m_catalog.put(p_entryNumber, p_action);
		
		return replacedAction;
	}
	
	/**
	 * @return Node class type identifier String
	 */
	public abstract String getNodeTypeIdentifierString();
	
	/**
	 * @return serialized XML
	 */
	public Element serialize()
	{
		Logger.v(TAG, "Serializing node " + getDescString());
		
		Element nodeElement = new Element(C_STR_NODELIST_NODE);

		Attribute nodeIdAttribute = new Attribute(C_STR_NODELIST_ID, getNodeIdString());
		Attribute nodeStatusAttribute = new Attribute(C_STR_NODELIST_STATUS, getNodeStatusString());
		Attribute nodeLastSeenAttribute = new Attribute(C_STR_NODELIST_LASTSEEN, getLastSeen().toString(SysUtils.getDateTimeFormatter()));
		Attribute nodeTypeAttribute = new Attribute(C_STR_NODELIST_TYPE, getNodeTypeIdentifierString());

		nodeElement.addAttribute(nodeIdAttribute);
		nodeElement.addAttribute(nodeStatusAttribute);
		nodeElement.addAttribute(nodeLastSeenAttribute);
		nodeElement.addAttribute(nodeTypeAttribute);

		Element nodeName = new Element(C_STR_NODELIST_NAME);
		nodeName.appendChild(getName());
		Element nodeSnId = new Element(C_STR_NODELIST_SNID);
		nodeSnId.appendChild(getSynetIdString());
		Element nodeManufac = new Element(C_STR_NODELIST_MANUFACTURER);
		nodeManufac.appendChild(getManufacturerIdString());
		Element nodeRevis = new Element(C_STR_NODELIST_REVISION);
		nodeRevis.appendChild(getRevisionString());
		Element nodeProfile = new Element(C_STR_NODELIST_PROFILE);
		nodeProfile.appendChild(getprofileString());

		nodeElement.appendChild(nodeName);
		nodeElement.appendChild(nodeSnId);
		nodeElement.appendChild(nodeManufac);
		nodeElement.appendChild(nodeRevis);
		nodeElement.appendChild(nodeProfile);
		
		return nodeElement;
	}
	
	/**
	 * Deserialize the node based on xml
	 * 
	 * @param p_xml
	 * @return
	 */
	private boolean deserialize(Element p_xml)
	{
		if (p_xml == null)
		{
		    Logger.e(TAG, "deserializing null xml element");
			return false;
		}
		
		if (!p_xml.getLocalName().equals(C_STR_NODELIST_NODE))
		{
		    Logger.e(TAG, "deserializing incorrect root element");
		    return false;
		}
		
		m_nodeID = XmlUtils.getXmlAttributeLong(p_xml, C_STR_NODELIST_ID);
		
		if (m_nodeID != 0)
		{
			m_dateLastSeen = XmlUtils.getXmlAttributeDateTime(p_xml, C_STR_NODELIST_LASTSEEN);
			m_name = XmlUtils.getXmlElementString(p_xml, C_STR_NODELIST_NAME);
			m_synetID = XmlUtils.getXmlElementInteger(p_xml, C_STR_NODELIST_SNID);
			m_manufacturerID = XmlUtils.getXmlElementInteger(p_xml, C_STR_NODELIST_MANUFACTURER);
			m_profile = XmlUtils.getXmlElementInteger(p_xml, C_STR_NODELIST_PROFILE);
			m_revision = XmlUtils.getXmlElementInteger(p_xml, C_STR_NODELIST_REVISION);
			
			
			return true;
		}
		return false;
	}
	
	
	/**
	 * deserialize any actions
	 * 
	 * @param p_xml
	 * @return
	 */
	protected boolean deserializeActions(Element p_xml)
	{
	    Element actionsElement = p_xml.getFirstChildElement("actions");
        if (actionsElement != null)
        {
            Elements actionElements = actionsElement.getChildElements("action");
            
            setNumTotalFunctions(actionElements.size());
            
            for (int i=0; i<actionElements.size(); ++i)
            {
                Element actionElement = actionElements.get(i);
                
                NodeAction action = new NodeAction(actionElement, getNodeId(), i+1);
                
                setNodeAction(action.getFunctionId(), action);
                
                Elements paramElements = actionElement.getChildElements("parameter");
                
                action.setNumParameters(paramElements.size());
                
                for (int p=0; p<paramElements.size(); ++p)
                {
                    Element paramElement = paramElements.get(p);
                    
                    NodeParameter param = new NodeParameter(paramElement, 
                            action.getActionId(),
                            getNodeId(),
                            i+1,
                            p+1);
                    
                    setParameter(i+1, p+1, param);
                }
            }
            
        }
        return true;
	}
	
	/**
	 * Reset the node to it's initial state
	 */
	public void reset()
	{
		setName(UNKNOWN_NAME);
		clearCatalog();
		
		m_isUnknown = true;
		m_investigationStatus = EsnInvestigationStatusEnum.UNKNOWN;
		m_dateLastSeen = new DateTime();
		m_timeNextInvestigation = new DateTime();
		m_numInvestigationRetries = 0;
		
		m_synetID = -1;
		m_manufacturerID = -1;
		m_profile = -1;
		m_revision = -1;
	}
	
	public abstract int getCodeUpdatePageSize();
	
	/**
	 * @param node
	 * @return
	 */
	public boolean isEqualTo(NodeBase node, boolean p_compareId)
	{
	   boolean retVal = true;
	   /*
	    *  private DateTime m_dateLastSeen; // The last time this node was seen
    private String m_name = UNKNOWN_NAME;
    private boolean m_isUnknown; // True if no active messages have been received
    private long m_nodeID; // The node ID

    private EsnInvestigationStatusEnum m_investigationStatus;
    private DateTime m_timeNextInvestigation;
    private int m_numInvestigationRetries;

    private boolean m_isBeingInvestigated = false;
    private boolean m_hasFullCatalog = false;
    private boolean m_hasFullParameters = false;

    private int m_synetID;
    private int m_manufacturerID;
    private int m_profile;
    private int m_revision;

    private HashMap<Integer, NodeAction> m_catalog = new HashMap<Integer, NodeAction>();
    private int m_numTotalFunctions = 0;
	    */
	   
	   retVal &= m_name.equals(node.getName());
	   if (p_compareId)
	   {
	       retVal &= m_nodeID == node.getNodeId();
	   }
	   retVal &= m_synetID == node.getSynetId();
	   retVal &= m_manufacturerID == node.getManufacturerId();
	   retVal &= m_profile == node.getProfile();
	   retVal &= m_revision == node.getRevision();
	   retVal &= getNumTotalFunctions() == node.getNumTotalFunctions();
	   retVal &= getActionCount() == node.getActionCount();
	   
	   for( int i : m_catalog.keySet())
	   {
	       INodeAction a1 = getNodeAction(i);
	       INodeAction a2 = node.getNodeAction(i);
	       
	       if (!(a2 != null && a1.isEqualTo(a2, p_compareId)))
	       {
	           retVal = false;
	       }
	   }
	   
	   return retVal;
	}
}
