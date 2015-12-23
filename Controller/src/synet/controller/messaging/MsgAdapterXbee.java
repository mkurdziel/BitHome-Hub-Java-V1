/**
 * 
 */
package synet.controller.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TooManyListenersException;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import synet.controller.NodeManager;
import synet.controller.messaging.messages.Msg;
import synet.controller.messaging.messages.MsgConstants;
import synet.controller.messaging.messages.MsgSystemNodelistReceive;
import synet.controller.messaging.messages.MsgTx;
import synet.controller.messaging.messages.MsgXbeeTxStatus;
import synet.controller.messaging.messages.MsgXbeeTxStatus.EsnTxDeliveryStatus;
import synet.controller.nodes.NodeBase;
import synet.controller.nodes.NodeZigbee;
import synet.controller.utils.Logger;
import synet.controller.utils.SysUtils;

/**
 * Adapter for connecting to Zigbee networks over an
 * Xbee usb serial adapter
 * 
 * @author kur57360
 *
 */
public class MsgAdapterXbee extends MsgAdapterBase implements SerialPortEventListener {

    private static final int PACKET_START_BYTE = 0x7e;

    private enum EsnPacketState {
        PACKET_START, // Waiting for Initial packet byte 
        SIZE_MSB, // Waiting for MSB of frame size
        SIZE_LSB, // Waiting for LSB of frame size 
        API, // Waiting for the API code 
        DATA, // Waiting for the frame data 
        CHECKSUM; // Waiting for the frame checksum
    }

    private static final String TAG = "MsgAdapterXbee";
    private static final String PORT_WIN = "COM15";
    private static final String PORT_MAC = "/dev/cu.usbserial-AH0015BR";
    private static final String PORT_LINUX = "/dev/ttyUSB0";

    private HashMap<Integer, MsgTx> m_msgHashMap = new HashMap<Integer, MsgTx>(256);

    private int m_currentFrameId = 1;
    private CommPortIdentifier m_commPort = null;
    private SerialPort m_serialPort = null;

    // Serial reading
    private InputStream m_serialIn = null;
    private OutputStream m_serialOut = null;

    // Packet decoding
    private EsnPacketState m_packetState = EsnPacketState.PACKET_START;
    private int m_packetSize = 0;
    private int m_packetDataIndex = 0;
    private int m_packetChecksum = 0; // short for unsigned byte
    private int m_packetAPI = 0;
    private int[] m_packetData = null;
    private int m_sendChecksum;
    private String m_strPortName;

    private MsgFactoryXbee m_msgFactoryXbee;

    private NodeZigbee m_broadcastNode = new NodeZigbee(0xFFFF, 0xFFFE);

    /**
     * Default constructor
     * @param p_commPort 
     */
    public MsgAdapterXbee(String p_commPort)
    {
        m_msgFactoryXbee = new MsgFactoryXbee();

        if (p_commPort != null)
        {
            Logger.v(TAG, "using comm port argument " + p_commPort);
            m_strPortName = p_commPort;
        }
        else
        {
            if(SysUtils.isWindows())
            {
                m_strPortName = PORT_WIN;
            }
            else if(SysUtils.isMac())
            {
                m_strPortName = PORT_MAC;
            }
            else if(SysUtils.isLinux())
            {
                m_strPortName = PORT_LINUX;
            }
            else 
            {
                Logger.e(TAG, "defined port not supported for this OS");
            }
        }
    }

    @Override
    public boolean startAdapater() {
        Logger.d(TAG, "Starting adapter");

        // Setup the base
        startBase();



        // First try connecting to the predefined port
        if (tryConnect(m_strPortName))
        {
            return true;
        }

        // Then try connecting to the first available serial port
        HashSet<CommPortIdentifier> availablePorts = getAvailableSerialPorts();
        for(CommPortIdentifier aPort : availablePorts)
        {
            if (!aPort.getName().contains("Bluetooth") && tryConnect(aPort.getName()))
            {
                return true;
            }
        }

        Logger.w(TAG, "Unable to connect to a serial port");
        return false;
    }

    @Override
    public void stopAdapter() {
        Logger.d(TAG, "Stopping adapter");

        // Stop the base
        stopBase();

        disconnect(m_serialPort);
    }
    /**
     * Disconnect the serial port from the event listener
     * @param p_serialPort
     */
    private void disconnect(SerialPort p_serialPort) {
        if (p_serialPort != null)
        {
            Logger.d(TAG, "disconnecting " + p_serialPort.getName());
            p_serialPort.removeEventListener();

            try
            {
                m_serialIn.close();
                m_serialOut.close();
            }
            catch (IOException e)
            {
                Logger.e(TAG, "disconnect: could not disconnect streams", e);
            }

            p_serialPort.close();

            m_serialIn = null;
            m_serialOut = null;
        }
    }

    /**
     * Attempt to connect to a port based on its string name
     * @param p_portName
     * @return
     */
    private boolean tryConnect(String p_portName)
    {
        Logger.d(TAG, "Trying to connect to " + p_portName);

        try
        {
            m_commPort = CommPortIdentifier.getPortIdentifier(p_portName);
            if((m_serialPort = connect(m_commPort)) == null)
            {
                Logger.e(TAG, "Failed to connect to " + p_portName);
                return false;
            }
        }
        catch (NoSuchPortException e)
        {
            Logger.e(TAG, "Could not get predefined port " + p_portName);
            return false;
        } 
        return true;
    }

    /**
     * Connect the comm port
     * @param p_commPort
     * @return the SerialPort that is now open
     */
    private SerialPort connect(CommPortIdentifier p_commPort)
    {
        Logger.v(TAG, "connect - " + p_commPort.getName());

        SerialPort retVal = null;
        if (p_commPort.isCurrentlyOwned())
        {
            Logger.e(TAG, "connect - port " + p_commPort.getName() + " is currently in use");
        }
        else
        {
            try {
                CommPort commPort = p_commPort.open(this.getClass().getName(), 2000);

                if (commPort instanceof SerialPort)
                {
                    SerialPort serialPort = (SerialPort)commPort;
                    try {
                        serialPort.setSerialPortParams(115200,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);
                        serialPort.enableReceiveThreshold(1);
                        serialPort.enableReceiveTimeout(10);

                        m_serialIn = serialPort.getInputStream();
                        m_serialOut = serialPort.getOutputStream();

                        if (m_serialIn == null || m_serialOut == null)
                        {
                            Logger.e(TAG, "connect - cannot get serial ports");
                        }
                        else
                        {
                            serialPort.addEventListener(this);
                            serialPort.notifyOnDataAvailable(true);

                            retVal = serialPort;
                        }

                    } catch (UnsupportedCommOperationException e) {
                        Logger.e(TAG, "connect - cannot set serial parameters", e);
                    } catch (IOException e) {
                        Logger.e(TAG, "connect - IO Exception", e);
                    } catch (TooManyListenersException e) {
                        Logger.e(TAG, "connect - too many listeners", e);
                    }
                }
                else
                {
                    Logger.e(TAG, "connect - " + commPort.getName() + " is not serial port");
                }
            } catch (PortInUseException e) {
                Logger.e(TAG, "connect - port " + p_commPort.getName() + " is currently in use");
            }
        }
        return retVal;
    }

    /**
     * Get the available comm ports
     * @return HashSet of available comm ports
     */
    @SuppressWarnings("unchecked")
    private HashSet<CommPortIdentifier> getAvailableSerialPorts()
    {
        HashSet<CommPortIdentifier> h = new HashSet<CommPortIdentifier>();
        Enumeration<CommPortIdentifier> thePorts = CommPortIdentifier.getPortIdentifiers();
        while (thePorts.hasMoreElements()) {
            CommPortIdentifier com = thePorts.nextElement();
            switch (com.getPortType()) {
                case CommPortIdentifier.PORT_SERIAL:
                    try {
                        CommPort thePort = com.open("CommUtil", 50);
                        thePort.close();
                        h.add(com);
                        Logger.v(TAG, "getAvailableSerialPorts - " + thePort.getName() + " available");
                    } catch (PortInUseException e) {
                        Logger.d(TAG, "getAvailableSerialPorts - Port, "  + com.getName() +  ", is in use.");
                    } catch (Exception e) {
                        Logger.e(TAG, "getAvailableSerialPorts - Failed to open port " + com.getName(), e);
                    }
            }
        }
        return h;
    }

    /* (non-Javadoc)
     * @see gnu.io.SerialPortEventListener#serialEvent(gnu.io.SerialPortEvent)
     */
    @Override
    public void serialEvent(SerialPortEvent arg0)
    {
        //Logger.v(TAG, "Serial event detected");
        int data;

        try
        {
            if (m_serialIn != null && m_serialIn.available() > 0)
            {
                while (( data = m_serialIn.read()) > -1 )
                {
                    decodePacketByte(data);
                }
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            System.exit(-1);
        }        
    }

    /**
     * Decode a packet byte and help for the packet
     * 
     * @param data
     */
    private void decodePacketByte(int data)
    {
//        		Logger.v(TAG, "decoding packet byte: "+ String.format("0x%x (%c)", data, data));

        // Decode based on the current packet state
        switch(m_packetState)
        {
            case PACKET_START:
            {
                // Check for the zigbee packet start (7e)
                if(data == PACKET_START_BYTE)
                {
                    //				Logger.v(TAG, "decode - read packet start:" + String.format("0x%x", data));
                    m_packetState = EsnPacketState.SIZE_MSB;
                }
                else
                {
                    //				Logger.w(TAG, "decode - invalid packet start byte:" + String.format("0x%x", data));
                }
            }
            break;
            case SIZE_MSB:
            {
                //			Logger.v(TAG, "decoding size MSB:" + String.format("0x%x", data));
                m_packetSize |= (data << 8);
                m_packetState = EsnPacketState.SIZE_LSB;
            }
            break;
            case SIZE_LSB:
            {
                //			Logger.v(TAG, "decoding size LSB:" + String.format("0x%x", data));
                m_packetSize |= data;

                // Initialize a new data buffer for this packet
                m_packetData = new int[m_packetSize];

                //			Logger.v(TAG, "packet size:" + m_packetSize);
                m_packetState = EsnPacketState.API;
            }
            break;
            case API:
            {
                //			Logger.v(TAG, "decoding API:" + String.format("0x%x", data));
                // add to the checksum
                m_packetAPI = data;
                m_packetChecksum = addByte(m_packetChecksum, data);
                m_packetSize--; // API is part of packet size. We just want data size
                m_packetState = EsnPacketState.DATA;
            }
            break;
            case DATA:
            {
                //			Logger.v(TAG, "decoding data:" + String.format("0x%x", data));
                m_packetData[m_packetDataIndex++] = data;
                m_packetChecksum = addByte(m_packetChecksum, data);

                // If the packet data index has overrun our packet size, move on
                if (m_packetDataIndex == m_packetSize)
                {
                    m_packetState = EsnPacketState.CHECKSUM;
                }
            }
            break;
            case CHECKSUM:
                //			Logger.v(TAG, "decoding checksum:" + String.format("0x%x", data));
                int checksum = 255 - m_packetChecksum;
                if (data == checksum)
                {
                    //				Logger.v(TAG, "decoding complete. Full Packet received");
                    Msg msgRx = m_msgFactoryXbee.createMessage(m_packetAPI, m_packetData);

                    // If it is a TX response, process it. Otherwise fire a notification
                    if (msgRx instanceof MsgXbeeTxStatus)
                    {
                        processTxStatus((MsgXbeeTxStatus)msgRx);
                    }
                    else
                    {
                        fireMsgReceivedEvent(msgRx);
                    }
                }
                else
                {
                    Logger.v(TAG, "decoding failed. Checksum expected:" + String.format("0x%x", checksum));
                }
                resetDecode();
                break;

        }
    }

    /**
     * Process the TX status and mark the original message
     * whether it was sent or not
     * 
     * @param msgRx
     */
    private void processTxStatus(MsgXbeeTxStatus p_msgTxStatus) {

        MsgTx msgTx = null;
        synchronized(m_msgHashMap)
        {
            msgTx = m_msgHashMap.get(p_msgTxStatus.getFrameId());
        }
        if (msgTx != null)
        {
            if (p_msgTxStatus.getDeliveryStatus() == EsnTxDeliveryStatus.SUCCESS)
            {
                Logger.v(TAG, "processing Tx status for message: sent");
                msgTx.setIsSent();
            }
            else
            {
                Logger.v(TAG, "processing Tx status for message: error " + p_msgTxStatus.getDescription());
                msgTx.setErrorMsg(p_msgTxStatus.getDescription());
            }
            // Remove it from the map
            synchronized(m_msgHashMap)
            {
                m_msgHashMap.remove(p_msgTxStatus.getFrameId());
            }
        }
        else
        {
            Logger.w(TAG, "processTxStatus - could not find tx message with frame id " + p_msgTxStatus.getFrameId());
        }
    }

    /**
     * Reset all the decode variables
     */
    private void resetDecode()
    {
        m_packetState = EsnPacketState.PACKET_START;
        m_packetAPI = 0;
        m_packetChecksum = 0;
        m_packetData = null;
        m_packetDataIndex = 0;
        m_packetSize = 0;
    }

    /**
     * @param m_packetChecksum2
     * @param data
     */
    private int addByte(int p_lhs, int p_rhs)
    {
        p_lhs += p_rhs;
        if( p_lhs > 255)
        {
            p_lhs -= 256;
        }
        return p_lhs;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#getNodeClass()
     */
    @Override
    public Class<?> getNodeClass()
    {
        return NodeZigbee.class;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#getNodeClass()
     */
    @Override
    public String getNodeTypeIdentifierString()
    {
        return NodeZigbee.NODE_TYPE_STRING;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#sendMessage(synet.controller.messaging.messages.Msg)
     */
    @Override
    public void sendMessage(MsgTx p_msg)
    {
        sendMessage(p_msg, p_msg.getDestinationNode());
    }

    /**
     * Send a message to a specific node
     * 
     * @param p_msg
     * @param p_destinationNode
     */
    private void sendMessage(MsgTx p_msg, NodeBase p_destinationNode)
    {
        Logger.v(TAG, "sending message: " + p_msg.getMsgType());

        switch(p_msg.getAPI())
        {
            case MsgConstants.SN_API_SYSTEM_CATALOG_TRANSMIT:
            case MsgConstants.SN_API_SYSTEM_NODELIST_TRANSMIT:
                // Do nothing
                return;
            case MsgConstants.SN_API_DEVICE_STATUS_REQUEST:
            case MsgConstants.SN_API_CATALOG_REQUEST:
            case MsgConstants.SN_API_PARAMETER_REQUEST:
            case MsgConstants.SN_API_FUNCTION_TRANSMIT:
            case MsgConstants.SN_API_BOOTLOAD_TRANSMIT:
            {
                SendTxMessage(p_msg, (NodeZigbee) p_destinationNode);
            }
            break;
            default:
            {
                Logger.w(TAG, "sending unhandled message: " + p_msg.getMsgType() + " api: " + p_msg.getAPI());
            }
        }
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#broadcastMessage(synet.controller.messaging.messages.MsgTx)
     */
    @Override
    public void broadcastMessage(MsgTx p_msg) {
        sendMessage(p_msg, m_broadcastNode);
    }

    /**
     * Send a message using zigbee tx
     * 
     * @param p_msg
     */
    private void SendTxMessage(MsgTx p_msg, NodeZigbee p_node)
    {
        byte[] messageBytes = p_msg.getBytes();
        byte[] addr16 = p_node.getAddress16Bytes();
        byte[] addr64 = p_node.getAddress64Bytes();
        int frameId = getNextFrameId();

        // Size = 1 API + 
        // frame ID + 2 16addr + 8 64addr + radius +
        // options + n packetdata + checksum
        int size = 14 + messageBytes.length;

        Logger.d(TAG, "Sending TX message: " + p_msg.getDescription());

        try
        {
            sb( MsgFactoryXbee.C_XBEE_API_START_BYTE);
            sb( (size>>8) & 0xff); // Size MSB
            sb( size & 0xff); // Size LSB

            // Start the checksum here
            m_sendChecksum = 0;

            sb( MsgFactoryXbee.C_XBEE_API_ZIGBEE_TX_REQ); // API
            sb( frameId ); // Frame ID
            sb( addr64 ); // 64-bit address
            sb( addr16 ); // 64-bit address
            sb( 0x00 ); // Broadcast Radius
            sb( 0x00 ); // Options
            sb( messageBytes ); // Packet bytes
            sb( 0xff - (m_sendChecksum & 0xff) ); // Checksum

            // Hash the message so we can correlate the send notification
            synchronized(m_msgHashMap)
            {
                m_msgHashMap.put(frameId, p_msg);
            }
        }
        catch (IOException e)
        {
            Logger.e(TAG, "error sending tx message", e);
        }
    }

    /**
     * @return the next frame Id
     */
    private int getNextFrameId() {
        if(++m_currentFrameId > 255)
        {
            m_currentFrameId = 0;
        }
        return m_currentFrameId;
    }

    /**
     * Send byte and log it
     * 
     * @param p_byte
     * @throws IOException
     */
    private void sb(int p_byte) throws IOException
    {
//        		Logger.v(TAG, "sending byte: " + String.format("0x%x", (byte)p_byte));
        m_serialOut.write(p_byte);
        m_sendChecksum = addByte(m_sendChecksum, p_byte);
    }

    /**
     * Send bytes and log it
     * 
     * @param p_byte
     * @throws IOException
     */
    private void sb(byte[] p_bytes) throws IOException
    {
        for(byte i=0; i < p_bytes.length; ++i)
        {
            sb(p_bytes[i]);
        }
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#processSystemNodeListReceive(synet.controller.messaging.messages.MsgSystemNodelistReceive)
     */
    @Override
    public void processSystemNodeListReceive(MsgSystemNodelistReceive p_msg) {
        Logger.v(TAG, "Adding new node from NodeList Receive");

        NodeZigbee node = new NodeZigbee(p_msg.getXml());

        NodeManager.getInstance().addNewNode(node.getNodeId(), node);
    }
}
