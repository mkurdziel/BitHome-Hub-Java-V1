/**
 * 
 */
package synet.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import org.joda.time.DateTime;

import synet.controller.actions.ActionManager;
import synet.controller.actions.NodeParameter;
import synet.controller.configuration.Configuration;
import synet.controller.messaging.MsgDispatcher;
import synet.controller.messaging.messages.MsgBootloadResponse;
import synet.controller.messaging.messages.MsgBootloadTransmit;
import synet.controller.messaging.messages.MsgCatalogRequest;
import synet.controller.messaging.messages.MsgCatalogResponse;
import synet.controller.messaging.messages.MsgDeviceStatusResponse;
import synet.controller.messaging.messages.MsgDeviceStatusRequest;
import synet.controller.messaging.messages.MsgConstants.EsnAPIBootloadResponse;
import synet.controller.messaging.messages.MsgConstants.EsnAPIBootloadTransmit;
import synet.controller.messaging.messages.MsgDeviceStatusRequest.EsnRequestType;
import synet.controller.messaging.messages.MsgParameterRequest;
import synet.controller.messaging.messages.MsgParameterResponse;
import synet.controller.messaging.messages.MsgSystemCatalogTransmit;
import synet.controller.messaging.messages.MsgSystemNodelistTransmit;
import synet.controller.nodes.NodeBase;
import synet.controller.nodes.NodeBroadcast;
import synet.controller.nodes.NodeBase.EsnInvestigationStatusEnum;
import synet.controller.nodes.NodeBase.EsnStatusEnum;
import synet.controller.nodes.NodeUpdateStatus.BootloadStatusEnum;
import synet.controller.nodes.NodeUpdateFile;
import synet.controller.nodes.NodeUpdateStatus;
import synet.controller.utils.Logger;
import synet.controller.utils.Pair;

/**
 * 
 * @author mkurdziel
 */
public class NodeManager
{
    private static final String TAG = "NodeManager";
    public static final int C_QUERY_INTERVAL_MS = 1000 * 60 * 2; // 2 minutes
    public static final int C_INVESTIGATE_INTERVAL_MS = 100; // 100 ms
    public static final int C_INVESTIGATE_TIMEOUT = 1000 * 5; // 2 second
    public static final int C_INVESTIGATE_RETRIES = 3; // Retry 3 times

    /*
     * Member variables
     */
    private static NodeManager m_instance;

    // Random generator for random node IDs
    private Random m_randGenerator = new Random();

    private int m_threadWaitMs;
    private boolean m_isInvestigating;

    private NodeManagerThread m_nodeManagerThread = new NodeManagerThread();
    private HashMap<Long, NodeBase> m_nodeMap = new HashMap<Long, NodeBase>();
    private ArrayList<NodeBase> m_nodesToInvestigateList = new ArrayList<NodeBase>();
    private boolean m_isPeriodicCheck = true;

    private MsgDispatcher m_msgDispatcher;
    private ActionManager m_actionManager;

    private NodeBroadcast m_broadcastNode = new NodeBroadcast();

    private HashMap<Long, NodeUpdateStatus> m_updateStatusMap = new HashMap<Long, NodeUpdateStatus>();

    // Settings from the configuration
    private Configuration m_config;
    private int m_investigate_interval_ms;
    private int m_query_interval_ms;
    private int m_investigate_timeout_ms;
    private int m_investigate_retry_num;

    public static synchronized NodeManager getInstance()
    {
        if (m_instance == null)
        {
            m_instance = new NodeManager();
        }
        return m_instance;
    }

    /**
     * Private constructor
     */
    private NodeManager()
    {
        // By default, the thread uses the query interval
        setQueryInterval();
    }

    /**
     * Set the message dispatcher
     * 
     * @param p_msgDispatcher
     */
    public void setMsgDispatcher(MsgDispatcher p_msgDispatcher)
    {
        m_msgDispatcher = p_msgDispatcher;
    }

    /**
     * Set the action manager
     * 
     * @param p_actionManager
     */
    public void setActionManager(ActionManager p_actionManager)
    {
        m_actionManager = p_actionManager;
    }

    /**
     * @return the number of nodes being investigated. 
     */
    public int getNumNodesInvestigating()
    {
        return m_nodesToInvestigateList.size();
    }

    /**
     * @return node for broadcasting to network
     */
    public NodeBroadcast getBroadcastNode()
    {
        return m_broadcastNode;
    }

    /**
     * Start the node manager
     */
    public void start()
    {
        Logger.d(TAG, "starting");

        loadConfiguration();

        m_nodeManagerThread.start();
    }

    /**
     * Load any member variables from the configuration
     */
    private void loadConfiguration() {
        m_investigate_interval_ms = m_config.getInt("investigateIntervalMs", C_INVESTIGATE_INTERVAL_MS);
        m_query_interval_ms = m_config.getInt("queryIntervalMs", C_QUERY_INTERVAL_MS);
        m_investigate_timeout_ms = m_config.getInt("investigateTimeoutMs", C_INVESTIGATE_TIMEOUT);
        m_investigate_retry_num = m_config.getInt("investigateRetryNum", C_INVESTIGATE_RETRIES);
    }

    /**
     * Save any member variables to the configuration
     */
    private void saveConfiguration()
    {
        m_config.addProperty("investigateIntervalMs", m_investigate_interval_ms);
        m_config.addProperty("queryIntervalMs", m_query_interval_ms);
        m_config.addProperty("investigateTimeoutMs", m_investigate_timeout_ms);
        m_config.addProperty("investigateRetryNum", m_investigate_retry_num);
    }

    /**
     * Stop the node manager
     */
    public void stop()
    {
        m_nodeManagerThread.stopThread();

        saveConfiguration();

        m_instance = null;

        Logger.d(TAG, "stopped");
    }

    /**
     * Set the thread to use the query interval
     */
    private void setQueryInterval()
    {
        m_isInvestigating = false;
        m_threadWaitMs = C_QUERY_INTERVAL_MS;
    }

    /**
     * Set the thread to use the investigation interval
     */
    private void setInvestigateInterval()
    {
        if (!m_isInvestigating)
        {
            m_isInvestigating = true;
            m_threadWaitMs = C_INVESTIGATE_INTERVAL_MS;
            // Since the investigate interval is smaller than the query interval,
            // We need to notify the thread to start spinning at the new interval
            m_nodeManagerThread.notifyThread();
        }
    }

    /**
     * @return the interval we are querying at
     */
    public int getQueryInterval()
    {
        return m_threadWaitMs;
    }

    /**
     * Add a device status message
     * 
     * @param p_msg
     */
    public void addMsgDeviceStatus(MsgDeviceStatusResponse p_msg) 
    { 	
        // Since we've seen the node, update it's last seen time
        p_msg.getSourceNode().setLastSeen(new DateTime());

        // Handle the types of status messages
        switch (p_msg.getDeviceStatus())
        {
            case ACTIVE:
                processStatusActive(p_msg.getSourceNode());
                break;
            case INFO:
                processStatusInfo(p_msg);
                break;
            case HW_RESET:
                processStatusHwReset(p_msg.getSourceNode());
                break;
        }

        // The node list may have changed, pump an update
        sendNodeListUpdate();
    }

    /**
     * Add a catalog response message
     * 
     * @param p_msg
     */
    public void addMsgCatalogResponse(MsgCatalogResponse p_msg) 
    { 	
        NodeBase node = p_msg.getSourceNode();

        // If this is zero, it's just the base catalog
        if (p_msg.getEntryNumber() == 0)
        {
            if (node.getInvestigationStatus() == EsnInvestigationStatusEnum.INFO)
            {
                Logger.d(TAG, "received base catalog for " + node.getDescString() +
                        " with " + p_msg.getTotalEntries() + " entries");

                node.setNumTotalFunctions(p_msg.getTotalEntries());

                // Update the investigation status
                node.setInvestigationStatus(EsnInvestigationStatusEnum.FUNCTION);

            }
            else
            {
                Logger.w(TAG, "received catalog when node isn't being investigated");
            }
        }
        else
        {
            // Process an individual catalog entry
            m_actionManager.addNodeAction(
                    node,
                    p_msg.getEntryNumber(),
                    p_msg.getFunctionName(),
                    p_msg.getReturnType(),
                    p_msg.getNumParams(),
                    p_msg.getParamType());

            Logger.v(TAG, "adding node action " + p_msg.getEntryNumber() + " to " + node.getDescString());

            // If we have all the functions, increment the investigation status
            if(node.getNextUnknownNodeAction() == 0)
            {
                Logger.v(TAG, "No more unknown functions for " + node.getDescString());

                node.setInvestigationStatus(EsnInvestigationStatusEnum.PARAMETER);
            }
            else
            {
                Logger.v(TAG, "Next unknown function for " + node.getDescString() + " is " + node.getNextUnknownNodeAction());
            }
        }

        resetInvestigationAttempts(node);
    }


    /**
     * Add a parameter response message
     * 
     * @param p_msg
     */
    public void addMsgParameterResponse(MsgParameterResponse p_msg)
    {
        NodeBase node = p_msg.getSourceNode();

        // Process an individual catalog entry
        NodeParameter parameter = m_actionManager.addNodeParameter(
                node.getNodeAction(p_msg.getFunctionId()),
                p_msg.getParameterId(), 
                p_msg.getFunctionId(), 
                node.getNodeId(), 
                p_msg.getParameterName(), 
                p_msg.getDataType(), 
                p_msg.getValidationType(), 
                p_msg.getMinumumValue(), 
                p_msg.getMaximumValue(), 
                p_msg.getMaxStringLength(), 
                p_msg.getEnumValues());

        Logger.v(TAG, "adding parameter to " + node.getDescString());
        Logger.v(TAG, parameter.getDescription());

        // If we have all the parameters for this function, send a notification
        //		if(node.getNextUnknownParameter(parameter.getFunctionId()) == 0)
        //		{
        //			fireNodeFunctionAdded(node.getFunction(parameter.getFunctionId()));
        //		}

        // If we have all the functions, increment the investigation status
        if(node.getNextUnknownParameter() == null)
        {
            node.setInvestigationStatus(EsnInvestigationStatusEnum.COMPLETED);
            node.setIsUnknown(false);
        }
        else
        {
            Pair<Integer> pair = node.getNextUnknownParameter();
            Logger.v(TAG, "next unknown parameter for " + node.getDescString() + " is " + pair.first() + ":" + pair.second());
        }

        resetInvestigationAttempts(node);
    }

    /**
     * Check if the node needs to be investigated
     * 
     * @param p_node
     */
    private void checkForInvestigation(NodeBase p_node)
    {
        synchronized(m_nodesToInvestigateList)
        {
            if(p_node.getInvestigationStatus() != EsnInvestigationStatusEnum.COMPLETED && 
                    !m_nodesToInvestigateList.contains(p_node))
            {
                Logger.v(TAG, p_node.getDescString() + " needs investigating");

                addNodeForInvestigation(p_node);
            }
        }
    }

    /**
     * Add a node to be investigated
     * 
     * @param p_node
     */
    private void addNodeForInvestigation(NodeBase p_node)
    {
        synchronized(m_nodesToInvestigateList)
        {
            // Add the node to the investigation list
            if (!m_nodesToInvestigateList.contains(p_node))
            {
                Logger.v(TAG, "Adding " + p_node.getDescString() + " for investigation");

                m_nodesToInvestigateList.add(p_node);

                // Set the next investigation step to now so it starts right away
                resetInvestigationAttempts(p_node);
            }
            else
            {
                Logger.e(TAG, "re-adding node to investigation list: " + p_node.getDescString());
            }

            // Switch over to the investigation timer
            setInvestigateInterval();
        }
    }

    /**
     * Remove a node from investigation
     * 
     * @param p_node
     */
    private void removeNodeForInvestigation(NodeBase p_node)
    {
        synchronized(m_nodesToInvestigateList)
        {
            // remove the node to the investigation list
            if (m_nodesToInvestigateList.contains(p_node))
            {
                Logger.v(TAG, "Removing " + p_node.getDescString() + " for investigation");
                m_nodesToInvestigateList.remove(p_node);
            }
            else
            {
                Logger.e(TAG, "re-removing node from investigation list: " + p_node.getDescString());
            }

            if (m_nodesToInvestigateList.size() == 0)
            {
                // Switch over to the interval timer
                setQueryInterval();
            }
        }
    }

    /**
     * Reset the investigation information for a node
     * 
     * @param p_node
     */
    private void resetInvestigationAttempts(NodeBase p_node)
    {
        Logger.v(TAG, "Resetting investigation attempts for " + p_node.getDescString() + " function " + p_node.getNextUnknownNodeAction());
        p_node.setTimeNextInvestigation(new DateTime());
        p_node.setNumInvestigationRetries(0);
        //		Logger.v(TAG, p_node.getDescString() + " reset investigation attempts");
    }

    /**
     * Set the update attempts for a node
     * 
     * @param p_node
     */
    private void resetUpdateAttempts(NodeBase p_node)
    {
        if (m_updateStatusMap.containsKey(p_node.getNodeId()))
        {
            Logger.v(TAG, "Resetting update attempts for " + p_node.getDescString() + " function " + p_node.getNextUnknownNodeAction());
            NodeUpdateStatus status = m_updateStatusMap.get(p_node.getNodeId());
            status.setTimeNextUpdate((new DateTime()).plus(C_INVESTIGATE_TIMEOUT));
            status.setNumRetries(0);
        }
    }

    /**
     * Process a hardware reset
     * 
     */
    private void processStatusHwReset(NodeBase p_node)
    {
        Logger.d(TAG, p_node.getDescString() + " hardware reset");

        // First we check to see if this is a HW reset to start
        // a software update
        if (m_updateStatusMap.containsKey(p_node.getNodeId()))
        {
            NodeUpdateStatus status = m_updateStatusMap.get(p_node.getNodeId());
            // If this device is in the reset state, then we are on track
            if (status.getStatus() == BootloadStatusEnum.RESET)
            {
                // We have the proper restart, now we will send the request
                status.setStatus(BootloadStatusEnum.BOOTLOAD_REQUEST);
                
                sendNextUpdate(p_node, status);
            }
            // If this device is in the reset again, keep sending bootload requests
            else if (status.getStatus() == BootloadStatusEnum.BOOTLOAD_REQUEST)
            {
                Logger.i(TAG, p_node.getDescString() + " restarted unexpectidly. Sending another bootload request");

                sendNextUpdate(p_node, status);
            }
            else
            {
                Logger.w(TAG, p_node.getDescString() + " rebooted without an update complete confirmation");
                m_updateStatusMap.remove(p_node.getNodeId());
            }
        }
        else
        {
            // let's change the investigation status to unknown since the firmware
            // and catalog may have changed during the reboot;
            p_node.setInvestigationStatus(EsnInvestigationStatusEnum.UNKNOWN);
        }
    }

    /**
     * Update the node active status
     * 
     * @param p_node
     */
    private void processStatusActive(NodeBase p_node)
    {
        Logger.d(TAG, p_node.getDescString() + " is active");

        checkForInvestigation(p_node);
    }

    /**
     * @param p_msg
     */
    private void processStatusInfo(MsgDeviceStatusResponse p_msg)
    {
        Logger.d(TAG, "Processing status info for: " + p_msg.getSourceNode().getDescString());

        // Populate the node information
        NodeBase node = p_msg.getSourceNode();

        // Check to see if anything has changed or if this is unknown
        if (node.getIsUnknown() || (node.getRevision() != p_msg.getRevision()))
        {
            Logger.d(TAG, "Setting status info for: " + p_msg.getSourceNode().getDescString() + 
                    " revision: " + node.getRevision() + " to " + p_msg.getRevision());
            node.setManufacturerId(p_msg.getManufacturerID());
            node.setRevision(p_msg.getRevision());
            node.setSynetID(p_msg.getSynetID());
            node.setProfile(p_msg.getProfile());

            // Since it is unknown or has changed, we need the catalog
            node.setInvestigationStatus(EsnInvestigationStatusEnum.INFO);
        }
        // Use this to prevent from going from unknown to info to completed prematurely 
        else if (node.getInvestigationStatus() == EsnInvestigationStatusEnum.UNKNOWN)
        {
            //Logger.d(TAG, "Node Revision: " + node.getRevision() + " Msg Revision: " + p_msg.getRevision());
            // Otherwise the node is known and the revision is the same
            node.setInvestigationStatus(EsnInvestigationStatusEnum.COMPLETED);
        }
        else if (node.getInvestigationStatus() == EsnInvestigationStatusEnum.TIMEOUT)
        {
            node.setInvestigationStatus(EsnInvestigationStatusEnum.INFO);
        }

        checkForInvestigation(node);
    }

    /**
     * Returns true if the node is in need of investigation
     * @param p_node
     * @return
     */
    private boolean doesNodeNeedInvestigation(NodeBase p_node)
    {
        return (p_node.getInvestigationStatus() == EsnInvestigationStatusEnum.UNKNOWN ||
                p_node.getInvestigationStatus() == EsnInvestigationStatusEnum.TIMEOUT);
    }

    /**
     * @param p_nodeID
     * @return the node associated with the Node ID. Null if not found.
     */
    public NodeBase getNode(Long p_nodeID)
    {
        synchronized(m_nodeMap)
        {
            // Look the ID up in the node map
            if (m_nodeMap.containsKey(p_nodeID))
            {
                return m_nodeMap.get(p_nodeID);
            }
        }
        return null;
    }

    /**
     * Inner thread to query nodes for their activity
     */
    private class NodeManagerThread extends Thread
    {
        private static final String TAG = "NodeManagerThread";
        private boolean m_running = false;
        private DateTime m_nextBroadcast = new DateTime();

        @Override
        public void run()
        {
            Logger.d(TAG, "starting");

            m_running = true;


            // First time we run, we want to refresh the info to
            // make sure that the version numbers match up
            refreshNodeInfos();

            m_nextBroadcast.plus(m_threadWaitMs);

            synchronized(this)
            {
                // Run thread until stopped
                while(m_running)
                {
                    //Logger.v(TAG, "Investigation Loop");

                    try
                    {
                        // See if there are any unknown nodes to investigate
                        // Don't send out pings if we are investigating 
                        if (m_isInvestigating)
                        {
                            investigateNodes();
                        }
                        // Send the periodic check and increment the time
                        else if (m_isPeriodicCheck)
                        {
                            // Check to see if we need to broadcast
                            if (m_nextBroadcast.isBeforeNow())
                            {
                                refreshNodes();

                                m_nextBroadcast.plus(m_threadWaitMs);
                            }

                        }

                        // See if there is anything to be updated
                        checkNodesToUpdate();

                        // Wait for the next one
                        if (m_isPeriodicCheck || m_isInvestigating)
                        {
                            this.wait(m_threadWaitMs);
                        } 
                        else 
                        {
                            this.wait();
                        }
                    }
                    catch (InterruptedException e)
                    {
                        Logger.e(TAG, "interrupted", e);
                    }
                }
            }
            Logger.d(TAG, "exiting");
        }

        /**
         * Check all the update status and initiate any updates
         */
        private void checkNodesToUpdate()
        {
            // See if there is anything to be updated
            if (m_updateStatusMap.isEmpty() == false)
            {
                for(NodeUpdateStatus status : m_updateStatusMap.values())
                {
                    // If the status is unknown, it hasn't been started.
                    // Initate the reboot
                    if (status.getStatus() == BootloadStatusEnum.UNKNOWN)
                    {
                        rebootNode(status.getNode());
                        status.setStatus(BootloadStatusEnum.RESET);
                    }
                    else if(status.getTimeNextUpdate().isBeforeNow())
                    {
                        Logger.i(TAG, "Timed out. Resending update");
                        sendNextUpdate(status.getNode(), status);
                    }
                }
            }
        }


        /**
         * Send a broadcast request to refresh the node infos
         */
        private void refreshNodeInfos() {
            m_msgDispatcher.sendMessage(
                    new MsgDeviceStatusRequest(getBroadcastNode(), EsnRequestType.INFORMATION)
            );
        }

        /**
         * Investigate any nodes
         */
        private void investigateNodes()
        {
            synchronized(m_nodesToInvestigateList)
            {
                for(int i=0; i<m_nodesToInvestigateList.size(); ++i)
                {
                    investigateNode(m_nodesToInvestigateList.get(i));
                }
            }
        }


        /**
         * Investigate a single node
         * 
         * @param p_node
         */
        private void investigateNode(NodeBase p_node)
        {
            DateTime nextInvestigation = p_node.getTimeNextInvestigation();

            // Check to see if we've already retried too many times
            if (p_node.getNumInvestigationRetries() == C_INVESTIGATE_RETRIES)
            {
                Logger.i(TAG, "Investigating " + p_node.getDescString() + " has timed out");

                p_node.setInvestigationStatus(EsnInvestigationStatusEnum.TIMEOUT);
                removeNodeForInvestigation(p_node);
            }
            // First check to see that the current time is
            // beyond the next investigation checkpoint
            else if (nextInvestigation.isBeforeNow())
            {
                Logger.v(TAG, "Investigating " + p_node.getDescString() + " function " + p_node.getNextUnknownNodeAction() + " Now:" + DateTime.now() + " NextInvestigation:" + p_node.getTimeNextInvestigation());
                switch(p_node.getInvestigationStatus())
                {
                    // If unknown, we need to get some information
                    case UNKNOWN:
                    case TIMEOUT:
                    {
                        Logger.d(TAG, "Investigating INFO for " + p_node.getDescString());

                        MsgDeviceStatusRequest msg = new MsgDeviceStatusRequest(p_node, MsgDeviceStatusRequest.EsnRequestType.INFORMATION);

                        m_msgDispatcher.sendMessage(msg);		
                    }
                    break;
                    // If we have the info, we need to move on to get the catalog
                    case INFO:
                    {
                        Logger.d(TAG, "Investigating CATALOG for " + p_node.getDescString());

                        MsgCatalogRequest msg = new MsgCatalogRequest(p_node, MsgCatalogRequest.FULL_CATALOG_REQUEST);

                        m_msgDispatcher.sendMessage(msg);		
                    }
                    break;
                    // Query until we have all the functions
                    case FUNCTION:
                    {
                        int function = p_node.getNextUnknownNodeAction();

                        if (function != 0)
                        {
                            Logger.d(TAG, "Investigating FUNCTION " + function + " for " + p_node.getDescString());

                            MsgCatalogRequest msg = new MsgCatalogRequest(p_node, function);

                            m_msgDispatcher.sendMessage(msg);		
                        }
                        else
                        {
                            Logger.w(TAG, "Investigating full catalog for " + p_node.getDescString());
                        }
                    }
                    break;
                    // Query until we have all the parameters
                    case PARAMETER:
                    {
                        Pair<Integer> pair = p_node.getNextUnknownParameter();

                        if (pair != null)
                        {
                            Logger.d(TAG, "Investigating PARAMETER " + pair.first() + ":" + pair.second() + " for " + p_node.getDescString());

                            MsgParameterRequest msg = new MsgParameterRequest(p_node, pair.first(), pair.second());

                            m_msgDispatcher.sendMessage(msg);		
                        }
                        else
                        {
                            Logger.w(TAG, "Investigating full parameters for " + p_node.getDescString());
                        }
                    }
                    break;
                    default:
                    {
                        Logger.w(TAG, "An investigation attept is made in an unimplemented state: " + p_node.getInvestigationStatus() );
                    }
                    break;
                }

                if(p_node.getInvestigationStatus() == EsnInvestigationStatusEnum.COMPLETED)
                {
                    removeNodeForInvestigation(p_node);

                }
                else
                {
                    p_node.setNumInvestigationRetries(p_node.getNumInvestigationRetries()+1);
                    p_node.setTimeNextInvestigation(nextInvestigation.plus(C_INVESTIGATE_TIMEOUT));
                    Logger.v(TAG, "setting next investigation time for " + p_node.getDescString() + " function " + p_node.getNextUnknownNodeAction() + " to " + p_node.getTimeNextInvestigation());
                }

                // Broadcast that the catalog has changed
                sendCatalogUpdate(p_node);
                sendNodeListUpdate();
            }
        }

        /**
         * Stop the thread
         */
        private void stopThread()
        {
            m_running = false;

            // Alert the thread so it can exit
            synchronized(this)
            {
                this.notify();
            }

            Logger.d(TAG, "stopped");
        }

        /**
         * Notify the thread
         */
        private void notifyThread()
        {
            synchronized(this)
            {
                this.notifyAll();
            }
        }
    }

    /**
     * Add a new node that might need to be investigated
     * 
     * @param node
     */
    public void addNewNode(Long p_nodeId, NodeBase p_node)
    {
        Logger.v(TAG, "adding new node: " + p_node.getDescString());

        m_nodeMap.put(p_nodeId, p_node);

        // Wake up the thread and tell it to investigate
        m_nodeManagerThread.notifyThread();
    }

    /**
     * Send the node status request
     */
    public void refreshNodes()
    {
        m_msgDispatcher.sendMessage(
                new MsgDeviceStatusRequest(getBroadcastNode(), EsnRequestType.STATUS)
        );
    }

    /**
     * Remove a node from the Node Manager
     * 
     * @param p_nodeId
     * @return
     */
    public boolean removeNode(Long p_nodeId)
    {
        Logger.v(TAG, "removing node: " + String.format("0x%x", p_nodeId));

        NodeBase node = m_nodeMap.remove(p_nodeId);
        if (node != null)
        {
            m_nodesToInvestigateList.remove(node);
        }
        return node != null;
    }

    /**
     * @return the current list of nodes
     */
    public Collection<NodeBase> getNodeList()
    {
        return m_nodeMap.values();
    }

    public NodeBase[] getNodeArray()
    {
        return m_nodeMap.values().toArray(new NodeBase[m_nodeMap.size()]);
    }

    /**
     * @return true if the node manager should check periodically
     */
    public boolean getIsPeriodicCheck()
    {
        return m_isPeriodicCheck;
    }

    /**
     * Set whether there is a periodic check
     * 
     * @param p_isPeriodicCheck
     */
    public void setIsPeriodicCheck(boolean p_isPeriodicCheck)
    {
        m_isPeriodicCheck = p_isPeriodicCheck;
    }

    /**
     * Broadcast a node list update
     */
    private void sendNodeListUpdate()
    {
        MsgSystemNodelistTransmit msg = 
            new MsgSystemNodelistTransmit(m_broadcastNode, getNodeArray());
        m_msgDispatcher.sendMessage(msg);
    }

    /**
     * Broadcast the node catalog update
     * 
     * @param p_node
     */
    private void sendCatalogUpdate(NodeBase p_node)
    {
        MsgSystemCatalogTransmit msg = 
            new MsgSystemCatalogTransmit(m_broadcastNode, p_node);
        m_msgDispatcher.sendMessage(msg);
    }

    /**
     * Clear node and reinvestigate 
     * 
     * @param p_node
     */
    public void reinvestigateNode(NodeBase p_node)
    {
        // Reset the node to clear out it's information and 
        // remove it's catalog. Then investigate
        p_node.reset();
        addNodeForInvestigation(p_node);
    }

    /**
     * Set the configuration for the node manager
     * 
     * @param p_config
     */
    public void setConfiguration(Configuration p_config) {
        m_config = p_config;
    }

    /**
     * Update the node with the given file
     * 
     * @param p_node
     * @param p_strFilename
     */
    public void updateNode(NodeBase p_node, String p_strFileName)
    {
        Logger.i(TAG, "Updating node " + p_node.getDescString() + " with file " + p_strFileName);

        NodeUpdateFile file = new NodeUpdateFile(p_strFileName);
        if (file.parseFile())
        {
            Logger.i(TAG, "Success parsing node update file");
            // Create the update status, hash it, and wake up the
            // node manager to handle this
            NodeUpdateStatus status = new NodeUpdateStatus(p_node, file);

            // If the node is unknown and has only reported a HW reset,
            // this may be an initial load. Skip the reset and
            // go straight to waiting for confirmation
            if (p_node.getInvestigationStatus()== EsnInvestigationStatusEnum.UNKNOWN)
            {
                Logger.i(TAG, "Unknown node. Could be an initial software load. Skipping reset");
                status.setStatus(BootloadStatusEnum.BOOTLOAD_REQUEST);
            }
            // Hash this so the node manage thread can pick it up
            m_updateStatusMap.put(p_node.getNodeId(), status);
            
            // Change the query interval
            setInvestigateInterval();

            m_nodeManagerThread.notifyThread();

        }
        else
        {
            Logger.w(TAG, "Unable to parse update file");
        }
    }

    /**
     * Send out a reboot message to the node
     * 
     * @param p_node
     */
    public void rebootNode(NodeBase p_node)
    {
        Logger.i(TAG, "Rebooting node " + p_node.getDescString());
        MsgBootloadTransmit msg = new MsgBootloadTransmit(p_node, EsnAPIBootloadTransmit.REBOOT_DEVICE);
        m_msgDispatcher.sendMessage(msg);            
    }

    /**
     * Send out a update request message
     * 
     * @param p_node
     */
    public void sendUpdateRequest(NodeBase p_node)
    {
        Logger.i(TAG, "Requesting update for node " + p_node.getDescString());
        MsgBootloadTransmit msg = new MsgBootloadTransmit(p_node, EsnAPIBootloadTransmit.BOOTLOAD_REQUEST);
        m_msgDispatcher.sendMessage(msg);            
    }

    /**
     * Process a bootload response message
     * 
     * @param p_msg
     */
    public void addMsgBootloadResponse(MsgBootloadResponse p_msg)
    {
        NodeBase node = p_msg.getSourceNode();

        // First lets get the update status for this node
        if (m_updateStatusMap.containsKey(node.getNodeId()))
        {
            NodeUpdateStatus status = m_updateStatusMap.get(node.getNodeId());

            switch(p_msg.getBootloadResponse())
            {
                case BOOTLOAD_READY:
                {
                    Logger.v(TAG, "node is ready to update. Starting update.");
                    status.setStatus(BootloadStatusEnum.DATA_TRANSMIT);

                }
                break;
                case DATA_SUCCESS:
                {
                    Logger.v(TAG, String.format("Node update data success. %s Address: %x", node.getDescString(), p_msg.getMemoryAddress()));
                    status.markAsSent(p_msg.getMemoryAddress());
                }
                break;
                case BOOTLOAD_COMPLETE:
                {
                    Logger.v(TAG, "Node update complete: " + node.getDescString());
                    m_updateStatusMap.remove(node.getNodeId());
                    setQueryInterval();
                }
                break;
                case ERROR_ADDRESS:
                case ERROR_API:
                case ERROR_BOOTLOADAPI:
                case ERROR_BOOTLOADSTART:
                case ERROR_CHECKSUM:
                case ERROR_MY16_ADDR:
                case ERROR_PAGELENGTH:
                case ERROR_SIZE:
                case ERROR_SNAPI:
                case ERROR_SNSTART:
                case ERROR_START_BIT:
                {
                    Logger.w(TAG, "Received error from updating device " + node.getDescString() + " " + p_msg.getBootloadResponse()); 
                }
                break;
            }
            
            // We received an update message to reset the update attempts
            resetUpdateAttempts(node);
            
            // Send the next update if not complete
            if(p_msg.getBootloadResponse() != EsnAPIBootloadResponse.BOOTLOAD_COMPLETE)
            {

                sendNextUpdate(node, status);
            }
        }
        else
        {
            Logger.e(TAG, "received a bootload message when there is no bootload status");
            // Just reboot the node
            rebootNode(node);
        }
    }
    
    /**
     * Send the next update to the node
     * 
     * @param p_node
     * @param p_status
     */
    private void sendNextUpdate(NodeBase p_node, NodeUpdateStatus p_status)
    {
        switch(p_status.getStatus())
        {
            case RESET:
            case BOOTLOAD_REQUEST:
            {
                Logger.i(TAG, p_node.getDescString() + "Sending bootload request");
                sendUpdateRequest(p_node);
            }
            break;
            case DATA_TRANSMIT:
            {
                if (p_status.isComplete())
                {
                    sendUpdateDataComplete(p_node);
                }
                else
                {
                    sendUpdateDataNext(p_node, p_status);
                }
            }
            break;
            case UNKNOWN:
            {
                Logger.w(TAG, "send next update for unknown status state");
            }
            break;
        }
        
        p_status.setNumRetries(p_status.getNumRetries()+1);
        p_status.setTimeNextUpdate(p_status.getTimeNextUpdate().plus(C_INVESTIGATE_TIMEOUT));
        Logger.v(TAG, "setting next update time for " + p_node.getDescString() + " to " + p_status.getTimeNextUpdate());
    }

    /**
     * Tell the node that the update has completed
     * 
     * @param p_node
     */
    private void sendUpdateDataComplete(NodeBase p_node)
    {
        Logger.v(TAG, String.format("Sending update complete to %s", p_node.getDescString()));
        MsgBootloadTransmit msg = new MsgBootloadTransmit(p_node, EsnAPIBootloadTransmit.DATA_COMPLETE);
        m_msgDispatcher.sendMessage(msg); 
    }

    /**
     * Send the next chunk of update data
     * @param p_status
     */
    private void sendUpdateDataNext(NodeBase p_node, NodeUpdateStatus p_status)
    {
        int nextAddress = p_status.getNextAddress();

        byte[] dataBytes = new byte[p_node.getCodeUpdatePageSize()];

        int checksum = p_node.getCodeUpdatePageSize();
        checksum += (nextAddress>>8);
        checksum += (nextAddress & 0xff);
        for (int i=0; i<p_node.getCodeUpdatePageSize(); ++i)
        {
            dataBytes[i] = p_status.getDataByte(nextAddress + i);
            checksum += dataBytes[i];
        }

        // Now reduce check_sum to 8 bits
        while (checksum > 256)
            checksum -= 256;

        // now take the two's compliment
        checksum = 256 - checksum;

        Logger.v(TAG, String.format("Sending update address %x to %s", nextAddress, p_node.getDescString()));
        MsgBootloadTransmit msg = new MsgBootloadTransmit(
                p_node, 
                EsnAPIBootloadTransmit.DATA_TRANSMIT,
                dataBytes,
                nextAddress,
                checksum,
                p_node.getCodeUpdatePageSize()
        );
        m_msgDispatcher.sendMessage(msg); 
    }

    /**
     * Delete the node from the system
     * 
     * @param p_nodeId
     */
    public void deleteNode(long p_nodeId)
    {
        NodeBase node = m_nodeMap.get(p_nodeId);
        
        if (node != null)
        {
            Logger.i(TAG, "deleting node " + node.getDescString());
            // TODO: delete actions and parameters too
            m_nodeMap.remove(p_nodeId);
        }
        else
        {
            Logger.w(TAG, "trying to delete unknown node " + String.format("0x%x", p_nodeId));
        }
    }
    
    /**
     * @return a unique node id
     */
    public long getUniqueNodeId()
    {
        long uniqueId = (long)m_randGenerator.nextLong();

        // We need to synchronize this so that we know it's truly unique
        synchronized(m_nodeMap)
        {
            while(m_nodeMap.get(uniqueId) != null)
            {
                uniqueId = m_randGenerator.nextLong();
            }
        }
        return uniqueId;
    }
}
