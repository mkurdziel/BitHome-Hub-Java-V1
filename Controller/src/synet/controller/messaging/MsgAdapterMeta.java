/**
 * Meta message adapter for meta nodes
 */
package synet.controller.messaging;


import synet.controller.NodeManager;
import synet.controller.actions.INodeAction;
import synet.controller.actions.INodeParameter;
import synet.controller.actions.NodeAction;
import synet.controller.actions.NodeParameter;
import synet.controller.messaging.messages.MsgBootloadTransmit;
import synet.controller.messaging.messages.MsgCatalogRequest;
import synet.controller.messaging.messages.MsgCatalogResponse;
import synet.controller.messaging.messages.MsgConstants;
import synet.controller.messaging.messages.MsgDeviceStatusRequest;
import synet.controller.messaging.messages.MsgDeviceStatusResponse;
import synet.controller.messaging.messages.MsgParameterRequest;
import synet.controller.messaging.messages.MsgParameterResponse;
import synet.controller.messaging.messages.MsgSystemNodelistReceive;
import synet.controller.messaging.messages.MsgTx;
import synet.controller.messaging.messages.MsgConstants.EsnAPIDeviceStatusValue;
import synet.controller.nodes.MetaNode;
import synet.controller.nodes.NodeBase;
import synet.controller.utils.Logger;

/**
 * @author mkurdziel
 *
 */
public class MsgAdapterMeta extends MsgAdapterBase
{
    public static final String TAG = "MsgAdapterMeta";
    
    /**
     * 
     */
    public MsgAdapterMeta()
    {
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#broadcastMessage(synet.controller.messaging.messages.MsgTx)
     */
    @Override
    public void broadcastMessage(MsgTx p_pMsg)
    {
        sendMessage(p_pMsg);
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#getNodeClass()
     */
    @Override
    public Class<?> getNodeClass()
    {
        return MetaNode.class;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#getNodeTypeIdentifierString()
     */
    @Override
    public String getNodeTypeIdentifierString()
    {
        return MetaNode.NODE_TYPE_STRING;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#processSystemNodeListReceive(synet.controller.messaging.messages.MsgSystemNodelistReceive)
     */
    @Override
    public void processSystemNodeListReceive(MsgSystemNodelistReceive p_msg)
    {
        Logger.v(TAG, "Adding new node from NodeList Receive");
        
        MetaNode node = new MetaNode(p_msg.getXml());
        
        NodeManager.getInstance().addNewNode(node.getNodeId(), node);
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#sendMessage(synet.controller.messaging.messages.MsgTx)
     */
    @Override
    public void sendMessage(MsgTx p_msg)
    {
        switch(p_msg.getAPI())
        {
            case MsgConstants.SN_API_DEVICE_STATUS_REQUEST:
                processStatusRequest((MsgDeviceStatusRequest)p_msg);
                break;
            case MsgConstants.SN_API_CATALOG_REQUEST:
                processCatalogRequest((MsgCatalogRequest)p_msg);
                break;
            case MsgConstants.SN_API_PARAMETER_REQUEST:
                processParameterRequest((MsgParameterRequest)p_msg);
                break;
            case MsgConstants.SN_API_BOOTLOAD_TRANSMIT:
                processBootloadRequest((MsgBootloadTransmit)p_msg);
                break;
        }
    }

    /**
     * @param p_msg
     */
    private void processBootloadRequest(MsgBootloadTransmit p_msg)
    {
        switch (p_msg.getState())
        {
            case REBOOT_DEVICE:
                sendDeviceResetResponse(p_msg.getDestinationNode());
                sendDeviceStatusResponse(p_msg.getDestinationNode());
                break;
        }
    }

    /**
     * @param p_msg
     */
    private void processParameterRequest(MsgParameterRequest p_msg)
    {
        NodeBase node = p_msg.getDestinationNode();
        sendParameterResponse(node, node.getParameter(p_msg.getActionIndex(), p_msg.getParameterIndex()));
    }

    /**
     * @param p_msg
     */
    private void processCatalogRequest(MsgCatalogRequest p_msg)
    {
        // TODO: handle null values.
        // TODO: add an error response
        NodeBase node = p_msg.getDestinationNode();
        sendCatalogResponse(node, node.getNodeAction(p_msg.getActionIndex()), node.getNumTotalFunctions());
        
    }

    /**
     * @param p_msg
     */
    private void processStatusRequest(MsgDeviceStatusRequest p_msg)
    {
        switch(p_msg.getRequestType())
        {
            case INFORMATION:
                sendDeviceInfoResponse(p_msg.getDestinationNode());
                break;
            case STATUS:
                sendDeviceStatusResponse(p_msg.getDestinationNode());
                break;
        }
        
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#startAdapater()
     */
    @Override
    public boolean startAdapater()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#stopAdapter()
     */
    @Override
    public void stopAdapter()
    {

    }
    
    /**
     * Send the information response
     * 
     * @param p_node
     */
    private void sendDeviceResetResponse( NodeBase p_node )
    {
        // Send back the initial status information
        MsgDeviceStatusResponse msgStatusResponse = 
            new MsgDeviceStatusResponse(p_node, null, 
                    EsnAPIDeviceStatusValue.HW_RESET, 
                    p_node.getSynetId(), p_node.getManufacturerId(), 
                    p_node.getProfile(), p_node.getRevision());
        fireMsgReceivedEvent(msgStatusResponse);
    }
    
    
    /**
     * Send the information response
     * 
     * @param p_node
     */
    private void sendDeviceInfoResponse( NodeBase p_node )
    {
        // Send back the initial status information
        MsgDeviceStatusResponse msgStatusResponse = 
            new MsgDeviceStatusResponse(p_node, null, 
                    EsnAPIDeviceStatusValue.INFO, 
                    p_node.getSynetId(), p_node.getManufacturerId(), 
                    p_node.getProfile(), p_node.getRevision());
        fireMsgReceivedEvent(msgStatusResponse);
    }
    
    /**
     * Send the information response
     * 
     * @param p_node
     */
    private void sendDeviceStatusResponse( NodeBase p_node )
    {
        // Send back the initial status information
        MsgDeviceStatusResponse msgStatusResponse = 
            new MsgDeviceStatusResponse(p_node, null, 
                    EsnAPIDeviceStatusValue.ACTIVE, 
                    p_node.getSynetId(), p_node.getManufacturerId(), 
                    p_node.getProfile(), p_node.getRevision());
        fireMsgReceivedEvent(msgStatusResponse);
    }
    
    /**
     * @param p_node
     * @param p_action
     * @param p_numFunctions 
     */
    private void sendCatalogResponse( NodeBase p_node, INodeAction p_action, int p_numFunctions)
    {
        // Send the requested function
        MsgCatalogResponse msgCatalogResponse = 
            // total entries, entry number, num params, function name, param type
            new MsgCatalogResponse(p_node, null, p_numFunctions,
                    p_action.getFunctionId(),p_action.getNumParameters(),
                    p_action.getReturnType(), p_action.getName(),null); 
        fireMsgReceivedEvent(msgCatalogResponse);
    }

    /**
     * @param p_node
     * @param p_param
     */
    private void sendParameterResponse( NodeBase p_node, INodeParameter p_param)
    {
        MsgParameterResponse msgParamResponse = new 
        MsgParameterResponse(p_node, null, 
                p_param.getFunctionIndex(),
                p_param.getParameterIndex(),
                p_param.getName(),
                p_param.getDataType(),
                p_param.getValidationType(),
                p_param.getMaxStringLength(),
                p_param.getMinimumValue(),
                p_param.getMaximumValue(),
                p_param.getEnumValueMap());
        fireMsgReceivedEvent(msgParamResponse);
    }
}
