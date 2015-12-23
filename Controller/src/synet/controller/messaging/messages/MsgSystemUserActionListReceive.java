/**
 * 
 */
package synet.controller.messaging.messages;

import java.util.Date;

import nu.xom.Element;

import synet.controller.nodes.NodeBase;

/**
 * @author mkurdziel
 *
 */
public class MsgSystemUserActionListReceive extends Msg
{
	private static final String TAG = "MsgSystemUserActionListReceive";
    String m_actionType;
    Element m_xml;
    
    /**
     * @param p_sourceNode
     * @param p_destinationNode
     */
    public MsgSystemUserActionListReceive(String p_actionType,
            Element p_xml)
    {
        m_actionType = p_actionType;
        m_xml = p_xml;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.messages.Msg#getMsgType()
     */
    @Override
    public String getMsgType()
    {
        return TAG;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.messages.Msg#getAPI()
     */
    @Override
    public byte getAPI()
    {
        return MsgConstants.SN_API_SYSTEM_USERACTIONLIST_RECEIVE;
    }

    /**
     * @return the action type
     */
    public String getActionType()
    {
         return m_actionType;
    }
    
    /**
     * @return the element of the message
     */
    public Element getXml()
    {
        return m_xml;
    }
}
