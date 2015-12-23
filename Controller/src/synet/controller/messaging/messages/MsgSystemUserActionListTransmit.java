/**
 * 
 */
package synet.controller.messaging.messages;

import synet.controller.actions.ActionBase;
import synet.controller.actions.IAction;
import synet.controller.nodes.NodeBase;

/**
 * @author mkurdziel
 *
 */
public class MsgSystemUserActionListTransmit extends MsgTx
{
    private IAction[] m_userActions;
	private static final String MSG_TYPE = "User Action List Transmit";
    
    /**
     * @param p_destinationNode
     */
    public MsgSystemUserActionListTransmit(NodeBase p_destinationNode, IAction[] p_userActions)
    {
        super(p_destinationNode);
        
        m_userActions = p_userActions;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.messages.MsgTx#getBytes()
     */
    @Override
    public byte[] getBytes()
    {
        return null;
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
        return MsgConstants.SN_API_SYSTEM_USERACTIONLIST_TRANSMIT;
    }
    
    /**
     * @return the list of user actions
     */
    public IAction[] getUserActions()
    {
        return m_userActions;
    }

}
