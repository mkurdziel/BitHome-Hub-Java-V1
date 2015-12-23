/**
 * 
 */
package synet.controller.messaging.messages;

import nu.xom.Element;

/**
 * @author kur57360
 *
 */
public class MsgSystemNodelistReceive extends Msg {
	private static final String TAG = "MsgSystemNodelistReceive";
	private String m_nodeType;
	private Element m_xml;
	
	/**
	 * Default constructor
	 * 
	 * @param p_nodeType
	 * @param p_xml
	 */
	public MsgSystemNodelistReceive(String p_nodeType, Element p_xml) {
		m_nodeType = p_nodeType;
		m_xml = p_xml;
	}
	
	/**
	 * @return The node type to be deserialized
	 */
	public String getNodeType()
	{
		return m_nodeType;
	}
	
	/**
	 * @return the XML to be deserialized
	 */
	public Element getXml()
	{
		return m_xml;
	}

	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getAPI()
	 */
	@Override
	public byte getAPI() {
		return MsgConstants.SN_API_SYSTEM_NODELIST_RECEIVE;
	}

	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getMsgType()
	 */
	@Override
	public String getMsgType() {
		return TAG;
	}

}
