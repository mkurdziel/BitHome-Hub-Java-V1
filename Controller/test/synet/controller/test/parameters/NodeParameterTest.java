/**
 * 
 */
package synet.controller.test.parameters;

import static org.junit.Assert.*;
import junit.framework.TestCase;

import org.junit.Test;

import synet.controller.Protocol.EsnDataTypes;
import synet.controller.Protocol.EsnParamValidationTypes;
import synet.controller.actions.NodeParameter;

/**
 * @author mkurdziel
 *
 */
public class NodeParameterTest extends TestCase
{
    private static final String C_STR_TESTFILESDIR = "test/testFiles/";
    private final static short C_PARAMETERID = 1234;
    private final static short C_ACTIONID = 5678;
    private final static int C_PARAMINDEX = 2;
    private final static int C_FUNCTIONIDEX = 3;
    private final static long C_NODEID = 0xabcdefL;
    private final static String C_NAME = "testName";
    private final static EsnDataTypes C_DATATYPE = EsnDataTypes.BYTE;
    private final static EsnParamValidationTypes C_PARAMVALIDATION = EsnParamValidationTypes.UNSIGNED_FULL;
    private final static int C_MAXSTRINGLENGTH = 0;
    private final static long C_MINVALUE = 0;
    private final static long C_MAXVALUE = 200;
    
    /**
     * @return a default parameters
     */
    private NodeParameter getDefault()
    {
        return new NodeParameter(
                C_PARAMETERID, 
                C_ACTIONID,
                C_PARAMINDEX,
                C_FUNCTIONIDEX,
                C_NODEID,
                C_NAME, 
                C_DATATYPE, 
                C_PARAMVALIDATION, 
                C_MINVALUE, 
                C_MAXVALUE, 
                C_MAXSTRINGLENGTH, 
                null);
    }


    /**
     * Test method for {@link synet.controller.actions.NodeParameter#getParameterIndex()}.
     */
    @Test
    public void testGetParameterIndex()
    {
        NodeParameter p = getDefault();
        assertEquals(C_PARAMINDEX, p.getParameterIndex());
    }

    /**
     * Test method for {@link synet.controller.actions.NodeParameter#getFunctionIndex()}.
     */
    @Test
    public void testGetFunctionId()
    {
        NodeParameter p = getDefault();
        assertEquals(C_FUNCTIONIDEX, p.getFunctionIndex());
    }

    /**
     * Test method for {@link synet.controller.actions.NodeParameter#getNodeId()}.
     */
    @Test
    public void testGetNodeId()
    {
        NodeParameter p = getDefault();
        assertEquals(C_NODEID, p.getNodeId());
    }
}
