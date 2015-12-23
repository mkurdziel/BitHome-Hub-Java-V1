package synet.controller.nodes;

public class NodeNet extends NodeBase
{
	public static final String NODE_TYPE_STRING = "NET";
	
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
