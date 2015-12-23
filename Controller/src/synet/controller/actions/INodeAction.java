/**
 * 
 */
package synet.controller.actions;

import nu.xom.Element;

/**
 * @author mkurdziel
 *
 */
public interface INodeAction extends IAction
{
    /**
     * @return the Node ID this Action belongs to
     */
    public long getNodeId();

    /**
     * @return the Function ID this Action references
     */
    public int getFunctionId();


    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#getParameter(int)
     */
    public INodeParameter getParameter(int p_index);

    /**
     * @param p_parameter
     */
    public void addParameter(INodeParameter p_parameter);

    /**
     * @return the next unknown parameter
     */
    public int getNextUnknownParameter();

    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#getParameters()
     */
    public INodeParameter[] getParameters();

    /* (non-Javadoc)
     * @see synet.controller.actions.ActionBase#serialize()
     */
    public Element serialize();

    /* (non-Javadoc)
     * @see synet.controller.actions.IAction#getParameterCount()
     */
    public int getParameterCount();

    

}
