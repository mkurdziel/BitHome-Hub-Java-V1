package synet.controller.nodes;

import org.joda.time.DateTime;

/**
 * Class to hold the update information for the update process for a node
 * 
 * @author mkurdziel
 */
public class NodeUpdateStatus
{
    /**
     * Enumeration for the progress of this status
     */
    public static enum BootloadStatusEnum
    {
        UNKNOWN,
        RESET,
        BOOTLOAD_REQUEST,
        DATA_TRANSMIT
    }
    
    private NodeUpdateFile m_dataFile;
    private NodeBase m_node;
    private int m_nextAddress = 0;
    private BootloadStatusEnum m_statusEnum = BootloadStatusEnum.UNKNOWN;
    private DateTime m_timeNextInvestigation = null;
    private int m_numInvestigationRetries = 0;
    
    /**
     * @param p_node 
     * @param p_dataFile
     */
    public NodeUpdateStatus(NodeBase p_node, NodeUpdateFile p_dataFile)
    {
        m_dataFile = p_dataFile;
        m_node = p_node;
    }
    
    /**
     * Set the number of retries
     * 
     * @param p_retries
     */
    public void setNumRetries(int p_retries)
    {
        m_numInvestigationRetries = p_retries;
    }
    
    /**
     * @return the number of retries
     */
    public int getNumRetries()
    {
        return m_numInvestigationRetries;
    }
    
    /**
     * Set the time for the next investigation
     * 
     * @param p_time
     */
    public void setTimeNextUpdate(DateTime p_time)
    {
     
        m_timeNextInvestigation = p_time;
    }
    
    /**
     * @return the time of the next investigation
     */
    public DateTime getTimeNextUpdate()
    {
        if (m_timeNextInvestigation == null)
        {
            m_timeNextInvestigation = new DateTime();
        }
        return m_timeNextInvestigation;
    }
    
    /**
     * @return the bootload status
     */
    public BootloadStatusEnum getStatus()
    {
        return m_statusEnum;
    }
    
    /**
     * Set the update status
     * 
     * @param p_status
     */
    public void setStatus(BootloadStatusEnum p_status)
    {
        m_statusEnum = p_status;
    }
    
    /**
     * @return the node being updated
     */
    public NodeBase getNode()
    {
        return m_node;
    }

    /**
     * @return the next address to send
     */
    public int getNextAddress()
    {
        return m_nextAddress;
    }

    /**
     * Mark this memory address as sent
     * 
     * @param p_memoryAddress
     */
    public void markAsSent(int p_memoryAddress)
    {
       m_nextAddress = p_memoryAddress + m_node.getCodeUpdatePageSize(); 
    }
    
    /**
     * @return true if everything is marked as sent
     */
    public boolean isComplete()
    {
        return m_nextAddress > m_dataFile.getMaxAddress();
    }
    
    /**
     * @param p_address
     * @return
     */
    public byte getDataByte(int p_address)
    {
       return m_dataFile.getDataByte(p_address); 
    }
}
