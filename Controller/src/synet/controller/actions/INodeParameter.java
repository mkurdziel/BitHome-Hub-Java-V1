package synet.controller.actions;

public interface INodeParameter extends IActionParameter
{

    /**
     * @return the parameter ID, specified by the node
     */
    public int getParameterIndex();
    
    /**
     * @return the function ID, specified by the node
     */
    public int getFunctionIndex();
    
    /**
     * @return the node ID that owns this parameter
     */
    public long getNodeId();
    
    /** 
     * Deep compare this parameter to another parameter
     * 
     * @param p_param
     * @return
     */
    public boolean isEqualTo(INodeParameter p_param, boolean p_compareId);
}
