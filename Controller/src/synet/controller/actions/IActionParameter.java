package synet.controller.actions;

import nu.xom.Element;
import synet.controller.actions.ActionParameter.EsnActionParameterType;

public interface IActionParameter extends IParameter
{
        /**
         * @return the Action ID this parameter is associated with
         */
        public short getActionId();
        
        /**
         * @param p_actionId
         */
        public void setActionId(short p_actionId);
        
        /**
         * @return the Action parameter type
         */
        public EsnActionParameterType getParameterType();
        
        /**
         * Set the parameter type
         * 
         * @param p_type
         */
        public void setParameterType(EsnActionParameterType p_type);
        
         /** 
         * Deep compare this parameter to another parameter
         * 
         * @param p_param
         * @return
         */
        public boolean isEqualTo(IActionParameter p_param, boolean p_compareId);

}
