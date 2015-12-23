/**
 * 
 */
package synet.controller.nodes;

import java.util.ArrayList;

import synet.controller.actions.ActionBase;
import synet.controller.actions.INodeAction;
import synet.controller.actions.MetaNodeAction;
import synet.controller.actions.NodeAction;
import synet.controller.utils.Logger;
import synet.controller.utils.XmlUtils;

import nu.xom.Element;
import nu.xom.Elements;

/**
 * mkurdziel
 */
public class MetaNode extends NodeBase
{
    public static final String NODE_TYPE_STRING = "META";
    public static final String TAG = "MetaNode";
    
    /**
     * 
     */
    public MetaNode(long p_address64)
    {
        super(p_address64);
    }

    /**
     * @param p_xml
     */
    public MetaNode(Element p_xml)
    {
        super(p_xml);
    }

    /**
     * @param p_xml
     * @param p_deserializeActions
     */
    public MetaNode(Element p_xml, boolean p_deserializeActions)
    {
        super(p_xml, false);
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
        return NODE_TYPE_STRING;
    }
    
    /**
     * Update the software version
     */
    public void updateVersion()
    {
        this.setRevision(this.getRevision()+1);
    }
    
    /**
     * @param p_action
     */
    public void addAction(ActionBase p_action)
    {
        Logger.d(TAG, "Adding action " + p_action.getName());
        int totalActions = super.getActionCount();
        super.setNodeAction(totalActions+1, new MetaNodeAction(p_action, getNodeId(), totalActions+1));
        updateVersion();
    }
    
    @Override
    public INodeAction setNodeAction(int p_entryNumber, INodeAction p_action)
    {
        Logger.d(TAG, "setting node action " + p_entryNumber);
        return getNodeAction(p_entryNumber);
    }
    
    @Override
    public Element serialize()
    {
        Element root = super.serialize();
        System.out.println("Serializing Meta Node");
        System.out.println(root.toXML());
        return root;
    }
    
    @Override
    public int getNumTotalFunctions()
    {
        return super.getActionCount();
    }
}
