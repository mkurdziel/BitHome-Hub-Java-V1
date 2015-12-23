/**
 * 
 */
package synet.controller.messaging.messages;

import synet.controller.Protocol.EsnDataTypes;
import synet.controller.actions.INodeAction;
import synet.controller.nodes.NodeBase;
import synet.controller.utils.EBitConverter;
import synet.controller.utils.Logger;

/**
 * @author mkurdziel
 *
 */
public class MsgFunctionReceive extends Msg
{
    private static final String TAG = "MsgFunctionReceive";

    private int m_actionIndex = 0;
    private String m_stringValue = "";

    public MsgFunctionReceive(NodeBase p_sourceNode,
            NodeBase p_destinationNode,
            int[] p_data,
            int p_dataOffset)
    {
        super(p_sourceNode, p_destinationNode);

        p_dataOffset += 2;

        m_actionIndex = p_data[p_dataOffset++];

        INodeAction action = p_sourceNode.getNodeAction(m_actionIndex);

        if (action != null)
        {
            EsnDataTypes returnType = action.getReturnType();
            switch(action.getReturnType())
            {
                case STRING:
                {
                    StringBuilder sb = new StringBuilder();
                    char c;
                    while ((c=(char) p_data[p_dataOffset++]) != 0)
                    {
                        sb.append(c);
                    }
                    m_stringValue = sb.toString();
                }
                break;
                case BYTE:
                {
                    // TODO: make a return value parameter and set it here
                    int intValue = EBitConverter.loadValueGivenWidth(p_data, ++p_dataOffset, 1);
                    m_stringValue = String.valueOf(intValue);
                }
                break;
                case WORD:
                {
                    // TODO: make a return value parameter and set it here
                    int intValue = EBitConverter.loadValueGivenWidth(p_data, ++p_dataOffset, 2);
                    m_stringValue = String.valueOf(intValue);
                }
                break;
                case DWORD:
                {
                    // TODO: make a return value parameter and set it here
                    int intValue = EBitConverter.loadValueGivenWidth(p_data, ++p_dataOffset, 4);
                    m_stringValue = String.valueOf(intValue);
                }
                break;
                case QWORD:
                {
                    // TODO: make a return value parameter and set it here
                    int intValue = EBitConverter.loadValueGivenWidth(p_data, ++p_dataOffset, 8);
                    m_stringValue = String.valueOf(intValue);
                }
                break;
                case BOOL:
                {
                    // TODO: make a return value parameter and set it here
                    int intValue = EBitConverter.loadValueGivenWidth(p_data, ++p_dataOffset, 1);
                    m_stringValue = String.valueOf(intValue);
                }
                break;
            }
        }
        else
        {
            Logger.w(TAG, "function receive for action that we don't have: " + m_actionIndex);
        }
    }

    /**
     * @return the index of the action this is in response to
     */
    public int getActionIndex()
    {
        return m_actionIndex;
    }

    /**
     * @return the value as a String
     */
    public String getStringValue()
    {
        return m_stringValue;
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
        return MsgConstants.SN_API_FUNCTION_RECEIVE;
    }

}
