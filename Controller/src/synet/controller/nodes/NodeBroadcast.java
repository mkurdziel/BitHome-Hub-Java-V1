/**
 * 
 */
package synet.controller.nodes;

/**
 * @autddhor kur57360
 *
 */
public class NodeBroadcast extends NodeBase {
	public static final String NODE_TYPE_STRING = "BROADCAST";
	
	/**
	 * @return the node type identifier string
	 */
	@Override
	public String getNodeTypeIdentifierString()
	{
		return NODE_TYPE_STRING;
	}

    @Override
    public int getCodeUpdatePageSize()
    {
        return 0;
    }
}
