package synet.controller;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class Protocol {
	/**
	 * Enumeration for device status values
	 */
	public static enum EsnDataTypes
	{
		VOID (0x00),
		BYTE (0x01),
		WORD (0x02),
		STRING (0x03),
		DWORD (0x04),
		BOOL (0x05),
		QWORD (0x06);
		
		private static final Map<Integer,EsnDataTypes> lookup 
		= new HashMap<Integer,EsnDataTypes>();

		static {
			for(EsnDataTypes s : EnumSet.allOf(EsnDataTypes.class))
				lookup.put(s.getCode(), s);
		}

		private int code;

		private EsnDataTypes(int code) {
			this.code = code;
		}

		public int getCode() { return code; }

		public static EsnDataTypes get(int code) { 
			return lookup.get(code); 
		}
	}
	/**
	 * Enumeration for parameter validation types
	 */
	public static enum EsnParamValidationTypes
	{
		UNSIGNED_FULL (0),
		UNSIGNED_RANGE (1),
		ENUMERATED (2),
		MAX_STRING_LEN (3),
		BOOL (4),
		SIGNED_FULL (10),
		SIGNED_RANGE (11),
		DATE_TIME (20),
		UNKNOWN (0xFF);
		
		private static final Map<Integer,EsnParamValidationTypes> lookup 
		= new HashMap<Integer,EsnParamValidationTypes>();

		static {
			for(EsnParamValidationTypes s : EnumSet.allOf(EsnParamValidationTypes.class))
				lookup.put(s.getCode(), s);
		}

		private int code;

		private EsnParamValidationTypes(int code) {
			this.code = code;
		}

		public int getCode() { return code; }

		public static EsnParamValidationTypes get(int code) { 
			return lookup.get(code); 
		}
	}
}
