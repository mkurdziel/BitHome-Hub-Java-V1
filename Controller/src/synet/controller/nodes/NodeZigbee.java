/**
 * 
 */
package synet.controller.nodes;

import synet.controller.utils.XmlUtils;
import nu.xom.Element;

/**
 * Represents a zigbee device
 * 
 * @author mkurdziel
 *
 */
public class NodeZigbee extends NodeBase
{
    public static final String NODE_TYPE_STRING = "ZIGBEE";

    public final String C_STR_XML_ADDR16 = "addr16";

    private int m_address16;
    private byte[] m_address16Bytes = null;
    private byte[] m_address64Bytes = null;

    /**
     * Address constructor
     * 
     * @param p_address64
     * @param p_address16
     */
    public NodeZigbee(long p_address64, int p_address16)
    {
        super(p_address64);

        m_address16 = p_address16;
    }

    /**
     * @param p_xml
     */
    public NodeZigbee(Element p_xml)
    {
        super(p_xml);

        deserialize(p_xml);
    }

    /**
     * @return the 16-bit address
     */
    public int getAddress16()
    {
        return m_address16;
    }

    /**
     * @return the 16-bit address in bytes
     */
    public byte[] getAddress16Bytes()
    {
        if (m_address16Bytes == null)
        {
            m_address16Bytes = new byte[]{ 
                    (byte)(m_address16 >>> 8),
                    (byte)(m_address16 >>> 0)
            };
        }
        return m_address16Bytes;
    }

    /**
     * @return the 64 bit address in bytes
     */
    public byte[] getAddress64Bytes()
    {
        if (m_address64Bytes == null)
        {
            long nodeID = getNodeId();

            m_address64Bytes = new byte[] {
                    (byte)(nodeID >>> 56),
                    (byte)(nodeID >>> 48),
                    (byte)(nodeID >>> 40),
                    (byte)(nodeID >>> 32),
                    (byte)(nodeID >>> 24),
                    (byte)(nodeID >>> 16),
                    (byte)(nodeID >>> 8),
                    (byte)(nodeID >>> 0)
            };

        }
        return m_address64Bytes;
    }

    /**
     * @return the node type identifier string
     */
    @Override
    public String getNodeTypeIdentifierString()
    {
        return NODE_TYPE_STRING;
    }

    /* (non-Javadoc)
     * @see synet.controller.nodes.NodeBase#serialize()
     */
    public Element serialize()
    {
        Element base = super.serialize();

        // Serialize the 16-bit address
        Element addr16Element = new Element(C_STR_XML_ADDR16);
        addr16Element.appendChild(String.format("0x%x", getAddress16()));
        base.appendChild(addr16Element);

        return base;
    }

    /* (non-Javadoc)
     * @see synet.controller.nodes.NodeBase#deserialize(nu.xom.Element)
     */
    private boolean deserialize(Element p_xml)
    {

        Integer ad16 = XmlUtils.getXmlElementInteger(p_xml, C_STR_XML_ADDR16); 
        // Deserialize the 16-bit address

        if (ad16 != null)
        {
            m_address16 = ad16;
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see synet.controller.nodes.NodeBase#getCodeUpdatePageSize()
     */
    @Override
    public int getCodeUpdatePageSize()
    {
        return 64;
    }
}
