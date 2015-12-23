/**
 * 
 */
package synet.controller.test.parameters;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import junit.framework.TestCase;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

import org.junit.Test;

import synet.controller.Protocol.EsnDataTypes;
import synet.controller.Protocol.EsnParamValidationTypes;
import synet.controller.actions.ActionParameter;
import synet.controller.actions.ActionParameter.EsnActionParameterType;
import synet.controller.actions.IActionParameter;
import synet.controller.test.parameters.ParameterBaseTest.Parameter;


/**
 * @author mkurdziel
 *
 */
public class ActionParameterTest extends TestCase
{
    private static final String C_STR_TESTFILESDIR = "test/testFiles/";
    private final static EsnActionParameterType C_ACTIONPARAMTYPE = EsnActionParameterType.INPUT; 
    private final static short C_PARAMETERID = 1234;
    private final static short C_ACTIONID = 5678;
    private final static String C_NAME = "testName";
    private final static EsnDataTypes C_DATATYPE = EsnDataTypes.BYTE;
    private final static EsnParamValidationTypes C_PARAMVALIDATION = EsnParamValidationTypes.UNSIGNED_FULL;
    private final static int C_MAXSTRINGLENGTH = 0;
    private final static long C_MINVALUE = 0;
    private final static long C_MAXVALUE = 200;
    
    /**
     * @return a default parameters
     */
    private ActionParameter getDefault()
    {
        return new ActionParameter(
                C_PARAMETERID, 
                C_ACTIONID,
                C_NAME, 
                C_DATATYPE, 
                C_PARAMVALIDATION, 
                C_MINVALUE, 
                C_MAXVALUE, 
                C_MAXSTRINGLENGTH, 
                null);
    }
    
    /**
     * Test get action id
     */
    @Test
    public void testActionId()
    {
        ActionParameter p = getDefault();
        
        assertEquals(C_ACTIONID, p.getActionId());
        
        p.setActionId((short)987);
        
        assertEquals((short)987, p.getActionId());
    }
    
    /**
     * Test parameter type
     */
    @Test
    public void testParameterType()
    {
        ActionParameter p = getDefault();
        
        assertEquals(C_ACTIONPARAMTYPE, p.getParameterType());
        
        p.setParameterType(EsnActionParameterType.CONSTANT);
        
        assertEquals(EsnActionParameterType.CONSTANT, p.getParameterType());
        
    }
    
    /**
     * Test 
     * @throws IOException 
     * @throws ParsingException 
     * @throws ValidityException 
     */
    @Test
    public void testSerialize() throws IOException, ValidityException, ParsingException
    {
        ActionParameter p = getDefault();
        
        testSerDes(p);
        
        // Test max
        p.setActionId(Short.MAX_VALUE);
        testSerDes(p);
        
        // Test min
        p.setActionId(Short.MIN_VALUE);
        testSerDes(p);
        
        // Test parameter type
        p.setParameterType(EsnActionParameterType.CONSTANT);
        testSerDes(p);
        
    }
    
    /**
     * Test serialization and deserialization
     * 
     * @param p
     * @throws IOException
     * @throws ValidityException
     * @throws ParsingException
     */
    private void testSerDes(ActionParameter p) throws IOException, ValidityException, ParsingException
    {
        Element paramElement = p.serialize();
        
        // Write the output file
        Document xmlDoc = new Document(paramElement);
        
        String xmlFileName = C_STR_TESTFILESDIR + "ActionParameterTestSerialize.xml";
        
        FileOutputStream fos = new FileOutputStream(xmlFileName);
        
        Serializer output = new Serializer(fos, "ISO-8859-1");
        
        output.write(xmlDoc);
        
        fos.close(); 
        
        // Read the file back in
        Builder parser = new Builder();
        
        File inFile = new File(xmlFileName);
        
        Document xmlDocIn = parser.build(inFile);
        
        // Create a new element with the XML
        ActionParameter p2 = new ActionParameter(xmlDocIn.getRootElement(), p.getActionId());
        // Compare the raw xml
        assertTrue(paramElement.toXML().equals(xmlDocIn.getRootElement().toXML()));
        // Compare the resulting parameter
        assertTrue(p.isEqualTo(p2, true));
    }
}
