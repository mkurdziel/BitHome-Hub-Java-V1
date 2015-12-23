/**
 * 
 */
package synet.controller.test.parameters;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import synet.controller.actions.IParameter;
import synet.controller.actions.ParameterBase;

/**
 * @author mkurdziel
 *
 */
public class ParameterBaseTest extends TestCase
{
    private static final String C_STR_TESTFILESDIR = "test/testFiles/";
    private final static short C_PARAMETERID = 1234;
    private final static String C_NAME = "testName";
    private final static EsnDataTypes C_DATATYPE = EsnDataTypes.BYTE;
    private final static EsnParamValidationTypes C_PARAMVALIDATION = EsnParamValidationTypes.UNSIGNED_FULL;
    private final static int C_MAXSTRINGLENGTH = 0;
    private final static long C_MINVALUE = 0;
    private final static long C_MAXVALUE = 200;
    

    /**
     * @return a default parameters
     */
    private Parameter getDefault()
    {
        return new Parameter(
                C_PARAMETERID, 
                C_NAME, 
                C_DATATYPE, 
                C_PARAMVALIDATION, 
                C_MINVALUE, 
                C_MAXVALUE, 
                C_MAXSTRINGLENGTH, 
                null);
    }
    
    /**
     * Test method for {@link synet.controller.actions.ParameterBase#getName()}.
     * Test method for {@link synet.controller.actions.ParameterBase#setName(java.lang.String)}.
     */
    @Test
    public void testName()
    {
        Parameter p = getDefault();
        
        assertEquals(C_NAME, p.getName());
        
        String name2 = "testName2";
        
        p.setName(name2);
        
        assertEquals(name2, p.getName());
    }
    
    /**
     * Test method for {@link synet.controller.actions.ParameterBase#getDataType()}.
     * Test method for {@link synet.controller.actions.ParameterBase#setDataType(synet.controller.Protocol.EsnDataTypes)}.
     */
    @Test
    public void testDataType()
    {
        Parameter p = getDefault();
        
        assertEquals(C_DATATYPE, p.getDataType());
        
        EsnDataTypes dt = EsnDataTypes.DWORD;
        
        p.setDataType(dt);
        
        assertEquals(dt, p.getDataType());
    }
    

    /**
     * Test method for {@link synet.controller.actions.ParameterBase#getValidationType()}.
     * Test method for {@link synet.controller.actions.ParameterBase#setValidationType(synet.controller.Protocol.EsnParamValidationTypes)}.
     */
    @Test
    public void testValidationType()
    {
        Parameter p = getDefault();
        
        assertEquals(C_PARAMVALIDATION, p.getValidationType());
        
        EsnParamValidationTypes dt = EsnParamValidationTypes.MAX_STRING_LEN;
        
        p.setValidationType(dt);
        
        assertEquals(dt, p.getValidationType());
    }

    /**
     * Test method for {@link synet.controller.actions.ParameterBase#getParameterId()}.
     */
    @Test
    public void testGetParameterId()
    {
        Parameter p = getDefault();
        
        assertEquals(C_PARAMETERID, p.getParameterId());
    }

    /**
     * Test method for {@link synet.controller.actions.ParameterBase#getParameterIdString()}.
     */
    @Test
    public void testGetParameterIdString()
    {
        Parameter p = getDefault();
        
        assertEquals(String.format("0x%x", C_PARAMETERID), p.getParameterIdString());
    }
    
    /**
     * Test method for {@link synet.controller.actions.ParameterBase#getDependentParamId()}.
     * Test method for {@link synet.controller.actions.ParameterBase#setDependentParamId(short)}.
     */
    @Test
    public void testDependentParamId()
    {
        Parameter p = getDefault();
        short depId = 987;
        
        p.setDependentParamId(depId);
        
        assertEquals(depId, p.getDependentParamId());
    }
    
    /**
     * Test method for {@link synet.controller.actions.ParameterBase#getMinimumValue()}.
     * Test method for {@link synet.controller.actions.ParameterBase#setMinimumValue(long)}.
     */
    @Test
    public void testMinimumValue()
    {
        Parameter p = getDefault();
        
        assertEquals(C_MINVALUE, p.getMinimumValue());
        
        long newMin = 1234;
        p.setMinimumValue(newMin);
        
        assertEquals(newMin, p.getMinimumValue());
    }
    
    /**
     * Test method for {@link synet.controller.actions.ParameterBase#getMaximumValue()}.
     * Test method for {@link synet.controller.actions.ParameterBase#setMaximumValue(long)}.
     */
    @Test
    public void testMaximumValue()
    {
        Parameter p = getDefault();
        
        assertEquals(C_MAXVALUE, p.getMaximumValue());
        
        long newMax = 1234;
        p.setMaximumValue(newMax);
        
        assertEquals(newMax, p.getMaximumValue());
    }
    
    /**
     * Test method for {@link synet.controller.actions.ParameterBase#getMaxStringLength()}.
     * Test method for {@link synet.controller.actions.ParameterBase#setMaxStringLength(int)}.
     */
    @Test
    public void testMaxStringLength()
    {
        Parameter p = getDefault();
        
        assertEquals(C_MAXSTRINGLENGTH, p.getMaxStringLength());
        
        int newMax = 50;
        
        p.setMaxStringLength(newMax);
        
        assertEquals(newMax, p.getMaxStringLength());
        
        // Try setting a negative and see if that's sllowed
        p.setMaxStringLength(-10);
        
        assertEquals(0, p.getMaxStringLength());
    }
    
    /**
     * Test all variations of byte
     */
    @Test
    public void testByte()
    {
        Parameter p = getDefault();
        
        p.setDataType(EsnDataTypes.BYTE);
        
        //////////////////////////////////////////////////////////// 
        // Test unsigned full 
        p.setValidationType(EsnParamValidationTypes.UNSIGNED_FULL);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertFalse(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("0"));
        assertEquals("0", p.getStrValue());
        assertEquals(0, p.getIntValue());
        // Test max
        assertTrue(p.setValue("255"));
        assertEquals("255", p.getStrValue());
        assertEquals(255, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("256"));
        // Test negative
        assertFalse(p.setValue("-1"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("0 - 255", p.getRange());
        
        
        //////////////////////////////////////////////////////////// 
        // Test signed full 
        p.setValidationType(EsnParamValidationTypes.SIGNED_FULL);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertTrue(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("-128"));
        assertEquals("-128", p.getStrValue());
        assertEquals(-128, p.getIntValue());
        // Test max
        assertTrue(p.setValue("127"));
        assertEquals("127", p.getStrValue());
        assertEquals(127, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("128"));
        // Test under run
        assertFalse(p.setValue("-129"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("-128 - 127", p.getRange());
        
        //////////////////////////////////////////////////////////// 
        // Test unsigned range 
        p.setValidationType(EsnParamValidationTypes.UNSIGNED_RANGE);
        p.setMinimumValue(10);
        p.setMaximumValue(100);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertFalse(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("10"));
        assertEquals("10", p.getStrValue());
        assertEquals(10, p.getIntValue());
        // Test max
        assertTrue(p.setValue("100"));
        assertEquals("100", p.getStrValue());
        assertEquals(100, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("101"));
        // Test under run
        assertFalse(p.setValue("9"));
        // Test negative
        assertFalse(p.setValue("-1"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("10 - 100", p.getRange());
        
        //////////////////////////////////////////////////////////// 
        // Test signed range 
        p.setValidationType(EsnParamValidationTypes.SIGNED_RANGE);
        p.setMinimumValue(-10);
        p.setMaximumValue(100);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertTrue(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("-10"));
        assertEquals("-10", p.getStrValue());
        assertEquals(-10, p.getIntValue());
        // Test max
        assertTrue(p.setValue("100"));
        assertEquals("100", p.getStrValue());
        assertEquals(100, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("101"));
        // Test under run
        assertFalse(p.setValue("-11"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("-10 - 100", p.getRange());
    }

    /**
     * Test all variations of word
     */
    @Test
    public void testWord()
    {
        Parameter p = getDefault();
        
        p.setDataType(EsnDataTypes.WORD);
        
        //////////////////////////////////////////////////////////// 
        // Test unsigned full 
        p.setValidationType(EsnParamValidationTypes.UNSIGNED_FULL);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertFalse(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("0"));
        assertEquals("0", p.getStrValue());
        assertEquals(0, p.getIntValue());
        // Test max
        assertTrue(p.setValue("65535"));
        assertEquals("65535", p.getStrValue());
        assertEquals(65535, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("65536"));
        // Test negative
        assertFalse(p.setValue("-1"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("0 - 65535", p.getRange());
        
        
        //////////////////////////////////////////////////////////// 
        // Test signed full 
        p.setValidationType(EsnParamValidationTypes.SIGNED_FULL);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertTrue(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("-32768"));
        assertEquals("-32768", p.getStrValue());
        assertEquals(-32768, p.getIntValue());
        // Test max
        assertTrue(p.setValue("32767"));
        assertEquals("32767", p.getStrValue());
        assertEquals(32767, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("32768"));
        // Test under run
        assertFalse(p.setValue("-32769"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("-32768 - 32767", p.getRange());
        
        //////////////////////////////////////////////////////////// 
        // Test unsigned range 
        p.setValidationType(EsnParamValidationTypes.UNSIGNED_RANGE);
        p.setMinimumValue(10);
        p.setMaximumValue(60000);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertFalse(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("10"));
        assertEquals("10", p.getStrValue());
        assertEquals(10, p.getIntValue());
        // Test max
        assertTrue(p.setValue("60000"));
        assertEquals("60000", p.getStrValue());
        assertEquals(60000, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("60001"));
        // Test under run
        assertFalse(p.setValue("9"));
        // Test negative
        assertFalse(p.setValue("-1"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("10 - 60000", p.getRange());
        
        //////////////////////////////////////////////////////////// 
        // Test signed range 
        p.setValidationType(EsnParamValidationTypes.SIGNED_RANGE);
        p.setMinimumValue(-10);
        p.setMaximumValue(30000);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertTrue(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("-10"));
        assertEquals("-10", p.getStrValue());
        assertEquals(-10, p.getIntValue());
        // Test max
        assertTrue(p.setValue("30000"));
        assertEquals("30000", p.getStrValue());
        assertEquals(30000, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("30001"));
        // Test under run
        assertFalse(p.setValue("-11"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("-10 - 30000", p.getRange());
    }
    
    /**
     * Test all variations of dword
     */
    @Test
    public void testDword()
    {
        Parameter p = getDefault();
        
        p.setDataType(EsnDataTypes.DWORD);
        
        //////////////////////////////////////////////////////////// 
        // Test unsigned full 
        p.setValidationType(EsnParamValidationTypes.UNSIGNED_FULL);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertFalse(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("0"));
        assertEquals("0", p.getStrValue());
        assertEquals(0, p.getIntValue());
        // Test max
        assertTrue(p.setValue("4294967295"));
        assertEquals("4294967295", p.getStrValue());
        assertEquals(4294967295L, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("4294967296"));
        // Test negative
        assertFalse(p.setValue("-1"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("0 - 4294967295", p.getRange());
        
        
        //////////////////////////////////////////////////////////// 
        // Test signed full 
        p.setValidationType(EsnParamValidationTypes.SIGNED_FULL);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertTrue(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("-2147483648"));
        assertEquals("-2147483648", p.getStrValue());
        assertEquals(-2147483648, p.getIntValue());
        // Test max
        assertTrue(p.setValue("2147483647"));
        assertEquals("2147483647", p.getStrValue());
        assertEquals(2147483647, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("2147483648"));
        // Test under run
        assertFalse(p.setValue("-2147483649"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("-2147483648 - 2147483647", p.getRange());
        
        //////////////////////////////////////////////////////////// 
        // Test unsigned range 
        p.setValidationType(EsnParamValidationTypes.UNSIGNED_RANGE);
        p.setMinimumValue(10);
        p.setMaximumValue(4000000000L);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertFalse(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("10"));
        assertEquals("10", p.getStrValue());
        assertEquals(10, p.getIntValue());
        // Test max
        assertTrue(p.setValue("4000000000"));
        assertEquals("4000000000", p.getStrValue());
        assertEquals(4000000000L, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("4000000001"));
        // Test under run
        assertFalse(p.setValue("9"));
        // Test negative
        assertFalse(p.setValue("-1"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("10 - 4000000000", p.getRange());
        
        //////////////////////////////////////////////////////////// 
        // Test signed range 
        p.setValidationType(EsnParamValidationTypes.SIGNED_RANGE);
        p.setMinimumValue(-2000000000);
        p.setMaximumValue(2000000000);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertTrue(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("-2000000000"));
        assertEquals("-2000000000", p.getStrValue());
        assertEquals(-2000000000, p.getIntValue());
        // Test max
        assertTrue(p.setValue("2000000000"));
        assertEquals("2000000000", p.getStrValue());
        assertEquals(2000000000, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("2000000001"));
        // Test under run
        assertFalse(p.setValue("-2000000001"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("-2000000000 - 2000000000", p.getRange());
    }
    
    /**
     * Test all variations of qword
     */
    @Test
    public void testQword()
    {
        Parameter p = getDefault();
        
        p.setDataType(EsnDataTypes.QWORD);
        
        //////////////////////////////////////////////////////////// 
        // Test unsigned full 
        p.setValidationType(EsnParamValidationTypes.UNSIGNED_FULL);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertFalse(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("0"));
        assertEquals("0", p.getStrValue());
        assertEquals(0, p.getIntValue());
        // Test middle
        assertTrue(p.setValue("4294967295"));
        assertEquals("4294967295", p.getStrValue());
        assertEquals(4294967295L, p.getIntValue());
        // Test max
        assertTrue(p.setValue("18446744073709551615"));
        assertEquals("18446744073709551615", p.getStrValue());
        assertEquals(0xffffffffffffffffL, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("18446744073709551616"));
        // Test negative
        assertFalse(p.setValue("-1"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("0 - 18446744073709551615", p.getRange());
        
        //////////////////////////////////////////////////////////// 
        // Test signed full 
        p.setValidationType(EsnParamValidationTypes.SIGNED_FULL);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertTrue(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("-9223372036854775808"));
        assertEquals("-9223372036854775808", p.getStrValue());
        assertEquals(-9223372036854775808L, p.getIntValue());
        // Test max
        assertTrue(p.setValue("9223372036854775807"));
        assertEquals("9223372036854775807", p.getStrValue());
        assertEquals(9223372036854775807L, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("9223372036854775808"));
        // Test under run
        assertFalse(p.setValue("-9223372036854775809"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("-9223372036854775808 - 9223372036854775807", p.getRange());
        
        //////////////////////////////////////////////////////////// 
        // Test unsigned range 
        p.setValidationType(EsnParamValidationTypes.UNSIGNED_RANGE);
        p.setMinimumValue(10);
        p.setMaximumValue(4000000000L);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertFalse(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("10"));
        assertEquals("10", p.getStrValue());
        assertEquals(10, p.getIntValue());
        // Test max
        assertTrue(p.setValue("4000000000"));
        assertEquals("4000000000", p.getStrValue());
        assertEquals(4000000000L, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("4000000001"));
        // Test under run
        assertFalse(p.setValue("9"));
        // Test negative
        assertFalse(p.setValue("-1"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("10 - 4000000000", p.getRange());
        
        //////////////////////////////////////////////////////////// 
        // Test signed range 
        p.setValidationType(EsnParamValidationTypes.SIGNED_RANGE);
        p.setMinimumValue(-2000000000);
        p.setMaximumValue(2000000000);
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertTrue(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        // Test min 
        assertTrue(p.setValue("-2000000000"));
        assertEquals("-2000000000", p.getStrValue());
        assertEquals(-2000000000, p.getIntValue());
        // Test max
        assertTrue(p.setValue("2000000000"));
        assertEquals("2000000000", p.getStrValue());
        assertEquals(2000000000, p.getIntValue());
        // Test over run
        assertFalse(p.setValue("2000000001"));
        // Test under run
        assertFalse(p.setValue("-2000000001"));
        // Test alpha
        assertFalse(p.setValue("abcd"));
        // Test range
        assertEquals("-2000000000 - 2000000000", p.getRange());
    }
    
    /**
     * Test all variations of string
     */
    @Test
    public void testString()
    {
        Parameter p = getDefault();
        
        p.setDataType(EsnDataTypes.STRING);
        p.setValidationType(EsnParamValidationTypes.MAX_STRING_LEN);
        
        p.setMaxStringLength(10);
        
        // Test is string
        assertTrue(p.getIsString());
        // Test is signed
        assertFalse(p.getIsSigned());
        // Test is integer
        assertFalse(p.getIsInteger());
        
        // Test string
        assertTrue(p.setValue("abcdefghij"));
        assertEquals("abcdefghij", p.getStrValue());
        // Test overrun
        assertFalse(p.setValue("abcdefghijk"));
        // Test range
        assertEquals("10-character string", p.getRange());
    }
    
    /**
     * Test all variations of enumeration
     */
    @Test
    public void testEnum()
    {
        Parameter p = getDefault();
        
        p.setDataType(EsnDataTypes.BYTE);
        p.setValidationType(EsnParamValidationTypes.ENUMERATED);
        
        HashMap<Integer, String> enums =  new HashMap<Integer, String>();
        enums.put(0, "On");
        enums.put(1, "Off");
        enums.put(2, "Blink");
        
        p.setEnumMap(enums);
        
        //////////////////////////////////////////////////////////// 
        // Test setting min
        assertTrue(p.setValue("0"));
        // Test max
        assertTrue(p.setValue("2"));
        // Test overrun
        assertFalse(p.setValue("3"));
        // Test underrun
        assertFalse(p.setValue("-1"));
        // Test alpha
        assertFalse(p.setValue("a"));
       
        assertEquals("Enumeration", p.getRange());
        
    }
    
    /**
     * Test all variations of bool
     */
    @Test
    public void testBool()
    {
        Parameter p = getDefault();
        
        p.setDataType(EsnDataTypes.BOOL);
        p.setValidationType(EsnParamValidationTypes.BOOL);
        
        // Test is string
        assertFalse(p.getIsString());
        // Test is signed
        assertFalse(p.getIsSigned());
        // Test is integer
        assertTrue(p.getIsInteger());
        
        // Test false
        assertTrue(p.setValue("0"));
        assertEquals("0", p.getStrValue());
        assertEquals(0, p.getIntValue());
        // Test true
        assertTrue(p.setValue("1"));
        assertEquals("1", p.getStrValue());
        assertEquals(1, p.getIntValue()); 
        // Test overrun
        assertFalse(p.setValue("2"));
        // Test range
        assertEquals("True or False", p.getRange());
    }
    
    /**
     * Test the equality test
     */
    @Test
    public void testEqualTo()
    {
        Parameter p1 = getDefault();
        Parameter p2 = getDefault();
        
        // Test basic equality
        assertTrue(p1.isEqualTo(p2));
        assertTrue(p2.isEqualTo(p1));
        
        // Data Type
        p1.setDataType(EsnDataTypes.BOOL);
        assertFalse(p1.isEqualTo(p2));
        p2.setDataType(EsnDataTypes.BOOL);
        assertTrue(p1.isEqualTo(p2));
        
        // Dependent Param
        p1.setDependentParamId((short)01);
        assertFalse(p1.isEqualTo(p2));
        p2.setDependentParamId((short)01);
        assertTrue(p1.isEqualTo(p2));
        
        // Value
        p1.setDataType(EsnDataTypes.BYTE);
        p2.setDataType(EsnDataTypes.BYTE);
        p1.setValue("01");
        assertFalse(p1.isEqualTo(p2));
        p2.setValue("01");
        assertTrue(p1.isEqualTo(p2));
        
        // max value
        p1.setMaximumValue(02L);
        assertFalse(p1.isEqualTo(p2));
        p2.setMaximumValue(02L);
        assertTrue(p1.isEqualTo(p2));
        
        // min value
        p1.setMinimumValue(02L);
        assertFalse(p1.isEqualTo(p2));
        p2.setMinimumValue(02L);
        assertTrue(p1.isEqualTo(p2));
        
        // name
        p1.setName("name");
        assertFalse(p1.isEqualTo(p2));
        p2.setName("name");
        assertTrue(p1.isEqualTo(p2));
        
        // validation type
        p1.setValidationType(EsnParamValidationTypes.UNSIGNED_FULL);
        assertFalse(p1.isEqualTo(p2));
        p2.setValidationType(EsnParamValidationTypes.UNSIGNED_FULL);
        assertTrue(p1.isEqualTo(p2));
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
        Parameter p = getDefault();
        
        testSerDes(p);
        
        // Test maximum values
        p.setDependentParamId(Short.MAX_VALUE);
        p.setMaximumValue(Long.MAX_VALUE);
        p.setMinimumValue(Long.MAX_VALUE);
        p.setMaxStringLength(Integer.MAX_VALUE);
        testSerDes(p);
        
        // Test minimum values
        p.setDependentParamId(Short.MIN_VALUE);
        p.setMaximumValue(Long.MIN_VALUE);
        p.setMinimumValue(Long.MIN_VALUE);
        p.setMaxStringLength(Integer.MIN_VALUE);
        testSerDes(p);
        
        // Test enumerations
        p.setDataType(EsnDataTypes.BYTE);
        p.setValidationType(EsnParamValidationTypes.ENUMERATED);
        HashMap<Integer, String> enums =  new HashMap<Integer, String>();
        enums.put(0, "On");
        enums.put(1, "Off");
        enums.put(2, "Blink");
        p.setEnumMap(enums);
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
    private void testSerDes(Parameter p) throws IOException, ValidityException, ParsingException
    {
        Element paramElement = p.serialize();
        
        // Write the output file
        Document xmlDoc = new Document(paramElement);
        
        String xmlFileName = C_STR_TESTFILESDIR + "ParameterBaseTestSerialize.xml";
        
        FileOutputStream fos = new FileOutputStream(xmlFileName);
        
        Serializer output = new Serializer(fos, "ISO-8859-1");
        
        output.write(xmlDoc);
        
        fos.close(); 
        
        // Read the file back in
        Builder parser = new Builder();
        
        File inFile = new File(xmlFileName);
        
        Document xmlDocIn = parser.build(inFile);
        
        // Create a new element with the XML
        Parameter p2 = new Parameter(xmlDocIn.getRootElement());
        // Compare the raw xml
        assertTrue(paramElement.toXML().equals(xmlDocIn.getRootElement().toXML()));
        // Compare the resulting parameter
        assertTrue(p.isEqualTo(p2));
    }
    
    /**
     * Implementation of the abstract ParameterBase class to test it
     */
    public class Parameter extends ParameterBase
    {

        public Parameter(
                short p_parameterId, 
                String p_strName,
                EsnDataTypes p_dataType,
                EsnParamValidationTypes p_paramValidataionType,
                long p_minimumValue, 
                long p_maximumValue,
                int p_maxStringLength, 
                HashMap<Integer, String> p_dctEnumValueByName)
        {
            super(p_parameterId, p_strName, p_dataType, p_paramValidataionType,
                    p_minimumValue, p_maximumValue, p_maxStringLength,
                    p_dctEnumValueByName);
        }

        public Parameter(Element p_xml)
        {
            super(p_xml);
            // TODO Auto-generated constructor stub
        }
        
        public boolean isEqualTo(ParameterBase p_param)
        {
            // TODO Auto-generated method stub
            return super.isEqualTo(p_param, true);
        }

        @Override
        public boolean isEqualTo(IParameter p_param, boolean p_compareId)
        {
            return super.isEqualTo(p_param, true);
        }
        
    }

}
