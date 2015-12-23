/**
 * 
 */
package synet.controller.messaging.messages;

import synet.controller.messaging.messages.MsgConstants.EsnAPIDeviceStatusValue;
import synet.controller.nodes.NodeBase;
import synet.controller.utils.EBitConverter;
import synet.controller.utils.Logger;

/**
 * Class representing a device status message
 * @author mkurdziel
 *
 */
public class MsgDeviceStatusResponse extends Msg
{
    private static final String MSG_TYPE = "Device Status";
    private static final String TAG = "MsgDeviceStatus";

    private EsnAPIDeviceStatusValue m_deviceStatus;
    private int m_synetID;
    private int m_manufacturerID;
    private int m_profile;
    private int m_revision;

    /**
     * Initialization constructor 
     * 
     * @param p_sourceNode
     * @param p_destinationNode
     * @param p_statusValue
     * @param p_synetId
     * @param p_manufacId
     * @param p_profile
     * @param p_revision
     */
    public MsgDeviceStatusResponse(
            NodeBase p_sourceNode, 
            NodeBase p_destinationNode, 
            EsnAPIDeviceStatusValue p_statusValue,
            int p_synetId,
            int p_manufacId,
            int p_profile,
            int p_revision)
    {
        super(p_sourceNode, p_destinationNode);


        m_deviceStatus = p_statusValue;

        m_synetID = p_synetId;
        m_manufacturerID = p_manufacId;
        m_profile = p_profile;
        m_revision = p_revision;
    }
    
    /**
     * Data constructor 
     * @param p_destinationNode 
     * @param p_sourceNode 
     * 
     * @param p_data
     * @param p_dataOffset
     */
    public MsgDeviceStatusResponse(NodeBase p_sourceNode, 
            NodeBase p_destinationNode, 
            int[] p_data, 
            int p_dataOffset)
    {
        super(p_sourceNode, p_destinationNode);


        m_deviceStatus = EsnAPIDeviceStatusValue.get(p_data[p_dataOffset+2]);

        Logger.v(TAG,"status:" + m_deviceStatus + " size:" + p_data.length);


        // Parse out any additional info if necessary
        if (m_deviceStatus == EsnAPIDeviceStatusValue.INFO)
        {
            if (p_data.length > (p_dataOffset + 9))
            {
                m_synetID = EBitConverter.toUInt16(p_data, p_dataOffset + 3);
                m_manufacturerID = EBitConverter.toUInt16(p_data, p_dataOffset + 5);
                m_profile = EBitConverter.toUInt16(p_data, p_dataOffset + 7);
                m_revision = EBitConverter.toUInt16(p_data, p_dataOffset + 9);
            }
            else
            {
                Logger.w(TAG, "Received poorly formed info message from " + p_sourceNode.getDescString());
            }

            Logger.d(TAG,"Status:" + p_sourceNode.getDescString()+ " - "
                    + m_deviceStatus +  
                    " SyNetID:"+m_synetID+" manufacID:" + m_manufacturerID +
                    " profile:" + m_profile + " revision:" + m_revision);
        }
    }

    /**
     * @return the device status
     */
    public EsnAPIDeviceStatusValue getDeviceStatus()
    {
        return m_deviceStatus;
    }

    /**
     * @return the synet ID
     */
    public int getSynetID()
    {
        return m_synetID;
    }

    /**
     * @return the manufacturer ID
     */
    public int getManufacturerID()
    {
        return m_manufacturerID;
    }

    /**
     * @return the device profile
     */
    public int getProfile()
    {
        return m_profile;
    }

    /**
     * @return the device revision
     */
    public int getRevision()
    {
        return m_revision;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.messages.Msg#getMsgType()
     */
    @Override
    public String getMsgType()
    {
        return MSG_TYPE;
    }

    /**
     * @return the API value for this message
     */
    @Override
    public byte getAPI()
    {
        return MsgConstants.SN_API_DEVICE_STATUS_RESPONSE;
    }
}
