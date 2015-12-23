/**
 * 
 */
package synet.controller.test;

import nu.xom.Element;
import synet.controller.nodes.NodeBase;

/**
 * @author kur57360
 *
 */
public class TestNode extends NodeBase
{

    /**
     * 
     */
    public TestNode()
    {
    }

    /**
     * @param p_xml
     */
    public TestNode(Element p_xml)
    {
        super(p_xml, true);
    }

    /**
     * @param p_nodeID
     */
    public TestNode(long p_nodeID)
    {
        super(p_nodeID);
    }

    /* (non-Javadoc)
     * @see synet.controller.nodes.NodeBase#getCodeUpdatePageSize()
     */
    @Override
    public int getCodeUpdatePageSize()
    {
        return 0;
    }

    /* (non-Javadoc)
     * @see synet.controller.nodes.NodeBase#getNodeTypeIdentifierString()
     */
    @Override
    public String getNodeTypeIdentifierString()
    {
        return "TEST";
    }

}
