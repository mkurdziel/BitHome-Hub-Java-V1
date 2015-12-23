/**
 * Responsible for all sending and receiving of messages for the controller
 */
package synet.controller.messaging;

import java.util.HashMap;
import java.util.LinkedList;

import synet.controller.NodeManager;
import synet.controller.actions.ActionManager;
import synet.controller.configuration.Configuration;
import synet.controller.messaging.messages.Msg;
import synet.controller.messaging.messages.MsgBootloadResponse;
import synet.controller.messaging.messages.MsgCatalogResponse;
import synet.controller.messaging.messages.MsgConstants;
import synet.controller.messaging.messages.MsgDeviceStatusResponse;
import synet.controller.messaging.messages.MsgFunctionReceive;
import synet.controller.messaging.messages.MsgFunctionTransmit;
import synet.controller.messaging.messages.MsgParameterResponse;
import synet.controller.messaging.messages.MsgSystemNodelistReceive;
import synet.controller.messaging.messages.MsgSystemUserActionListReceive;
import synet.controller.messaging.messages.MsgTx;
import synet.controller.nodes.NodeBroadcast;
import synet.controller.utils.Logger;

/**
 * @author mkurdziel
 *
 */
public class MsgDispatcher implements MsgReceivedEventListener {
    private static final String TAG = "MsgDispatcher";

    private LinkedList<Msg> m_msgQueueIn = new LinkedList<Msg>();
    private LinkedList<MsgTx> m_msgQueueOut = new LinkedList<MsgTx>();

    private MsgReceiverThread m_receiverThread = new MsgReceiverThread();
    private MsgSenderThread m_senderThread = new MsgSenderThread();

    private HashMap<Class<?>, MsgAdapterBase> m_msgAdapterMap = new HashMap<Class<?>, MsgAdapterBase>();
    private HashMap<Long, HashMap<Integer, MsgFunctionTransmit>> m_actionMsgMap = new HashMap<Long, HashMap<Integer,MsgFunctionTransmit>>();

    private static MsgDispatcher m_instance;
    private NodeManager m_nodeManager;
    private boolean m_isStarted = false;
    
    private ActionManager m_actionManager;

    // Configuration variables
    private Configuration m_config;

    /**
     * @return the MsgDispatcher instance
     */
    public static synchronized MsgDispatcher getInstance()
    {
        if (m_instance == null)
        {
            m_instance = new MsgDispatcher();
        }
        return m_instance;
    }

    /**
     * Private default constructor
     */
    private MsgDispatcher()
    {
        m_nodeManager = NodeManager.getInstance();
        m_actionManager = ActionManager.getInstance();
    }

    @Override
    public void handleMsgReceivedEvent(MsgReceivedEvent p_eventObject) {
        queueMsgIn(p_eventObject.getMessage());
    }

    /**
     * Add a message adapter to the dispatcher
     * 
     * @param p_adapter
     */
    public void addMsgAdapter(MsgAdapterBase p_adapter)
    {
        // Check to see if we already have an adapter for this message type
        if (m_msgAdapterMap.containsKey(p_adapter.getNodeClass()))
        {
            Logger.w(TAG, "Already have adapter for node type:" + p_adapter.getNodeClass());
        }
        else
        {
            m_msgAdapterMap.put(p_adapter.getNodeClass(), p_adapter);
        }

        // Listen to the adapter for messages
        p_adapter.setMsgReceivedListener(this);
    }

    /**
     * Start the dispatcher
     * @return true if the dispatcher was able to start with 
     * at least one adapter
     */
    public boolean start()
    {
        boolean retVal;

        retVal = startAdapters();

        if (retVal)
        {
            startThreads();
        }
        else
        {
            Logger.w(TAG, "Could not start the adapters");
        }

        m_isStarted = retVal;

        return m_isStarted;
    }

    /**
     * Stop the dispatcher
     */
    public void stop()
    {
        stopAdapters();
        stopThreads();
        
        m_instance = null;
    }

    /**
     * Start the reader and writer threads
     */
    private void startThreads()
    {
        m_receiverThread.start();
        m_senderThread.start();
    }

    /**
     * Stop the reader and writer threads
     */
    private void stopThreads()
    {
        Logger.d(TAG, "Stopping threads");

        m_receiverThread.stopReader();
        m_senderThread.stopSender();

        try
        {
            m_receiverThread.join();
            m_senderThread.join();
        }
        catch (InterruptedException e)
        {
            Logger.e(TAG, "interrupted waiting for thread to stop", e);
        }
    }

    /**
     * Start any adapters in the dispatcher
     * 
     * @return true if at least one adapter started successfully
     */
    private boolean startAdapters()
    {
        boolean retVal = false;
        boolean started;
        for( MsgAdapterBase adapter : m_msgAdapterMap.values().toArray(new MsgAdapterBase[m_msgAdapterMap.size()]))
        {
            // Set the configuration object for the adapter
            adapter.setConfiguration(m_config.subset("adapters." + adapter.getNodeTypeIdentifierString().toLowerCase()));

            started = adapter.startAdapater();

            // If the adapter could not start, remove it from the list
            if(!started)
            {
                m_msgAdapterMap.remove(adapter.getNodeClass());
            }
            retVal |= started;
        }
        return retVal;
    }

    /**
     * Stop any attached adapter
     */
    private void stopAdapters()
    {
        for (MsgAdapterBase adapter : m_msgAdapterMap.values())
        {
            adapter.stopAdapter();
        }
    }

    /**
     * Queue an incoming message
     * 
     * @param p_msg
     */
    private void queueMsgIn(Msg p_msg)
    {
        if (p_msg != null)
        {
            if (p_msg.getIsSynchronous())
            {
                m_receiverThread.processMsg(p_msg);
            }
            else
            {
                // Make sure the message queue is thread safe
                synchronized(m_msgQueueIn)
                {
                    Logger.v(TAG, "queuing incoming message: " + p_msg.getDescription());
                    m_msgQueueIn.add(p_msg);
                }

                // Wake up the reader thread
                synchronized(m_receiverThread)
                {
                    m_receiverThread.notify();
                }
            }
        }
    }

    /**
     * Send a message out of the system
     * 
     * @param p_msg
     */
    public void sendMessage(MsgTx p_msg)
    {
        synchronized (m_msgQueueOut)
        {
            Logger.v(TAG, "queuing outgoing message: " + p_msg.getDescription());
            m_msgQueueOut.add(p_msg);
        }

        // Wake up the sender thread
        synchronized (m_senderThread)
        {
            m_senderThread.notify();
        }
    }

    /**
     * Sends a message without going through the threads. 
     * Used for internal message routing only!
     * 
     * @param p_msg
     */
    public void sendSynchronous(MsgTx p_msg)
    {
        m_receiverThread.processMsg(p_msg);
    }

    /**
     * Message reader thread
     */
    private class MsgReceiverThread extends Thread
    {
        private static final String TAG = "MsgReaderThread";
        private boolean m_running = false;
        private Msg m_msg;

        @Override
        public void run()
        {
            Logger.d(TAG, "starting");
            m_running = true;

            // Loop until the thread is stopped
            while(m_running)
            {
                if (m_msgQueueIn.size() > 0)
                {
                    // Check if there is a queued message
                    synchronized (m_msgQueueIn)
                    {
                        // Remove the first message
                        m_msg = m_msgQueueIn.remove();
                    }

                    processMsg(m_msg);
                }
                else
                {
                    synchronized(this)
                    {
                        // Wait for this thread to be notified
                        try
                        {
                            this.wait();
                        }
                        catch (InterruptedException e)
                        {
                            Logger.w(TAG, "thread interrupted", e);
                        }
                    }
                }
            }
        }

        /**
         * Process a received message
         * @param mMsg
         */
        private void processMsg(Msg p_msg) {
            Logger.v(TAG, "processing message:" + p_msg.getMsgType());

            switch(p_msg.getAPI())
            {
                case MsgConstants.SN_API_DEVICE_STATUS_RESPONSE:
                {
                    m_nodeManager.addMsgDeviceStatus((MsgDeviceStatusResponse)p_msg);
                }
                break;
                case MsgConstants.SN_API_BOOTLOAD_RESPONSE:
                {
                    m_nodeManager.addMsgBootloadResponse((MsgBootloadResponse)p_msg);
                }
                break;
                case MsgConstants.SN_API_CATALOG_RESPONSE:
                {
                    m_nodeManager.addMsgCatalogResponse((MsgCatalogResponse)p_msg);
                }
                break;
                case MsgConstants.SN_API_PARAMETER_RESPONSE:
                {
                    m_nodeManager.addMsgParameterResponse((MsgParameterResponse)p_msg);
                }
                break;
                case MsgConstants.SN_API_SYSTEM_NODELIST_RECEIVE:
                {
                    // Since the adapters can create the specific nodes, and this to them
                    giveNodeListReceiveToAdapter((MsgSystemNodelistReceive)p_msg);
                }
                break;
                case MsgConstants.SN_API_SYSTEM_USERACTIONLIST_RECEIVE:
                {
                    m_actionManager.addMsgUserActionListRecieve((MsgSystemUserActionListReceive)p_msg);
                }
                break;
                case MsgConstants.SN_API_FUNCTION_RECEIVE:
                {
                    // Check to see if this receive has a corresponding transmit to match
                    // up with
                    MsgFunctionReceive msg = (MsgFunctionReceive)p_msg;
                    Logger.v(TAG, "Function receive value:" + msg.getStringValue());
                    correlateFunctionReceive(msg);
                }
                break;
                default:
                {
                    Logger.w(TAG, "message is unhandled");
                }
            }
        }

        /**
         * Correlate an incoming function receive with a possible matching
         * function transmit
         * 
         * @param p_msg
         */
        private void correlateFunctionReceive(MsgFunctionReceive p_msg)
        {
            synchronized(m_actionMsgMap)
            {
                HashMap<Integer, MsgFunctionTransmit> msgs = m_actionMsgMap.get(p_msg.getSourceNode().getNodeId());
                if (msgs != null)
                {
                    MsgFunctionTransmit msg = msgs.get(p_msg.getActionIndex());
                    if (msg != null)
                    {
                        Logger.v(TAG, "Found correlated function transmit for receive");
                        msgs.remove(p_msg.getActionIndex());
                        msg.setIsResponded(p_msg);
                    }
                }
            }
        }

        /**
         * Handle the node list message off to the proper adapter
         * 
         * @param pMsg
         */
        private void giveNodeListReceiveToAdapter(MsgSystemNodelistReceive p_msg) {
            boolean found = false;
            for (MsgAdapterBase adapter : m_msgAdapterMap.values())
            {
                if (adapter.getNodeTypeIdentifierString().equals(p_msg.getNodeType()))
                {
                    adapter.processSystemNodeListReceive(p_msg);
                    found = true;
                }
            }
            
            if (!found)
            {
                Logger.e(TAG, "None of the " + m_msgAdapterMap.size() + " is correct");
            }
        }

        /**
         * Stop the thread from running
         */
        public void stopReader()
        {
            m_running = false;

            // Wake up the reader so it can exit
            synchronized(this)
            {
                notify();
            }

            Logger.d(TAG, "stopping");
        }
    }

    /**
     * Message writer thread
     */
    private class MsgSenderThread extends Thread
    {
        private static final String TAG = "MsgSenderThread";
        private boolean m_running = false;
        private MsgTx m_msg;

        @Override
        public void run()
        {
            Logger.d(TAG, "starting");
            m_running = true;

            // Loop until the thread is stopped
            while(m_running)
            {
                if (m_msgQueueOut.size() > 0)
                {
                    // Check if there is a queued message
                    synchronized (m_msgQueueOut)
                    {
                        // Remove the first message
                        m_msg = m_msgQueueOut.remove();

                        // If it's an action request wiht a return type,
                        // hash it to correlate a response
                        if (m_msg.getAPI() == MsgConstants.SN_API_FUNCTION_TRANSMIT &&
                                ((MsgFunctionTransmit)m_msg).getNeedsReturn())
                        {
                            Logger.v(TAG, "msg has return type so hashing for response");
                            hashFunctionTransmit((MsgFunctionTransmit)m_msg);
                        }
                        sendToAdapter(m_msg);
                    }

                    Logger.v(TAG, "Sending " + m_msg.getDescription());
                }
                else
                {
                    synchronized(this)
                    {
                        // Wait for this thread to be notified
                        try
                        {
                            this.wait();
                        }
                        catch (InterruptedException e)
                        {
                            Logger.w(TAG, "thread interrupted", e);
                        }
                    }
                }
            }
        }

        /**
         * Save the function transmit message with a return type so we can
         * correlate it with the function response
         * 
         * @param p_msg
         */
        private void hashFunctionTransmit(MsgFunctionTransmit p_msg)
        {
            synchronized(m_actionMsgMap)
            {
                long nodeId = p_msg.getDestinationNode().getNodeId();
                HashMap<Integer, MsgFunctionTransmit> msgs = m_actionMsgMap.get(nodeId);
                // If there isn't a map for this node, create one
                if (msgs == null)
                {
                    msgs = new HashMap<Integer, MsgFunctionTransmit>();
                    m_actionMsgMap.put(nodeId, msgs);
                }
                msgs.put(p_msg.getActionIndex(), p_msg);
            }
        }

        /**
         * Send the message through the adapter
         * 
         * @param m_msg
         */
        private void sendToAdapter(MsgTx p_msg)
        {
            // Check that we have an adapter for this node destination

            // If it is a broadcast node, send it to each adapter
            if (p_msg.getDestinationNode() instanceof NodeBroadcast)
            {
                for( MsgAdapterBase msgAdapter : m_msgAdapterMap.values())
                {
                    msgAdapter.broadcastMessage(p_msg);
                }
            }
            else if (m_msgAdapterMap.containsKey(p_msg.getDestinationNode().getClass()))
            {
                m_msgAdapterMap.get(p_msg.getDestinationNode().getClass()).sendMessage(p_msg);
            }
            else
            {
                Logger.w(TAG, "No message adapter for type " + p_msg.getDestinationNode().getClass());
            }
        }

        /**
         * Stop the thread from running
         */
        public void stopSender()
        {
            m_running = false;

            // Wake up the reader so it can exit
            synchronized(this)
            {
                notify();
            }

            Logger.d(TAG, "stopping");
        }
    }

    /**
     * Set the configuration object for the Message Dispatcher
     * @param subset
     */
    public void setConfiguration(Configuration p_config) {
        m_config = p_config;
    }

}
