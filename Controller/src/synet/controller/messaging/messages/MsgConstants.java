package synet.controller.messaging.messages;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class MsgConstants {
	
	public final static int SYNET_START_BYTE = 0xA5;
	public final static int SYNET_INDEX_API = 1;
	
	/**
	 * Enumeration for message values
	 */
	public static enum EsnMsgValues
	{
		PACKET_START ((byte)0xA5);
		
		byte m_value = 0;
		EsnMsgValues(byte b) {m_value = b;}
		public byte getByte() { return m_value;}
	}
	
	/**
	 * Synet Message Values
	 */
	public static final byte SN_API_DEVICE_STATUS_REQUEST = 0x02;
	public static final byte SN_API_DEVICE_STATUS_RESPONSE = 0x03;
	public static final byte SN_API_BOOTLOAD_TRANSMIT = 0x04;
	public static final byte SN_API_BOOTLOAD_RESPONSE = 0x05;
	public static final byte SN_API_SET_INFO = 0x0A;
	public static final byte SN_API_SET_INFO_RESPONSE = 0x08;
	public static final byte SN_API_CATALOG_REQUEST = 0x10;
	public static final byte SN_API_CATALOG_RESPONSE = 0x11;
	public static final byte SN_API_PARAMETER_REQUEST = 0x12;
	public static final byte SN_API_PARAMETER_RESPONSE = 0x13;
	public static final byte SN_API_SYSTEM_NODELIST_TRANSMIT = 0x21;
	public static final byte SN_API_SYSTEM_NODELIST_RECEIVE = 0x22;
	public static final byte SN_API_SYSTEM_CATALOG_TRANSMIT = 0x23;
	public static final byte SN_API_SYSTEM_CATALOG_RECEIVE = 0x24;
	public static final byte SN_API_SYSTEM_USERACTIONLIST_TRANSMIT = 0x25;
	public static final byte SN_API_SYSTEM_USERACTIONLIST_RECEIVE = 0x26;
	public static final byte SN_API_FUNCTION_TRANSMIT = 0x40;
	public static final byte SN_API_FUNCTION_TRANSMIT_RESPONSE = 0x41;
	public static final byte SN_API_FUNCTION_RECEIVE = 0x60;
	public static final byte SN_API_FUNCTION_RECEIVE_RESPONSE = 0x61;
	
	
	/**
	 * Enumeration for device status values
	 */
	public static enum EsnAPIDeviceStatusValue
	{
		ACTIVE (0x00),
		HW_RESET (0x01),
		INFO (0x02);
		
		private static final Map<Integer,EsnAPIDeviceStatusValue> lookup 
		= new HashMap<Integer,EsnAPIDeviceStatusValue>();

		static {
			for(EsnAPIDeviceStatusValue s : EnumSet.allOf(EsnAPIDeviceStatusValue.class))
				lookup.put(s.getCode(), s);
		}

		private int code;

		private EsnAPIDeviceStatusValue(int code) {
			this.code = code;
		}

		public int getCode() { return code; }

		public static EsnAPIDeviceStatusValue get(int code) { 
			return lookup.get(code); 
		}
	}
	
	/**
	 * Enumeration for device status requests
	 */
	public static enum EsnAPIDeviceStatusRequest
	{
		STATUS_REQIEST ((byte)0x00),
		INFO_REQUEST ((byte)0x01);
		
		byte m_value = 0;
		EsnAPIDeviceStatusRequest(byte b) {m_value = b;}
	}
	
	/**
	 * Enumeration for bootload transmit
	 */
	public static enum EsnAPIBootloadTransmit
	{
		REBOOT_DEVICE (0x00),
		BOOTLOAD_REQUEST (0x01),
		DATA_TRANSMIT (0x03),
		DATA_COMPLETE (0x04);
		
		private static final Map<Integer,EsnAPIBootloadTransmit> lookup 
		= new HashMap<Integer,EsnAPIBootloadTransmit>();

		static {
			for(EsnAPIBootloadTransmit s : EnumSet.allOf(EsnAPIBootloadTransmit.class))
				lookup.put(s.getCode(), s);
		}

		private int code;

		private EsnAPIBootloadTransmit(int code) {
			this.code = code;
		}

		public int getCode() { return code; }

		public static EsnAPIBootloadTransmit get(int code) { 
			return lookup.get(code); 
		}
	}
	
	/**
	 * Enumeration for bootload response
	 */
	public static enum EsnAPIBootloadResponse
	{
		BOOTLOAD_READY (0x00),
		DATA_SUCCESS (0x01),
		BOOTLOAD_COMPLETE (0x02),
	    ERROR_START_BIT ((byte)0x03),
	    ERROR_SIZE ((byte)0x04),
	    ERROR_API ((byte)0x05),
	    ERROR_MY16_ADDR ((byte)0x06),
	    ERROR_BOOTLOADAPI ((byte)0x07),
	    ERROR_BOOTLOADSTART ((byte)0x08),
	    ERROR_PAGELENGTH ((byte)0x09),
	    ERROR_ADDRESS ((byte)0x0A),
	    ERROR_CHECKSUM ((byte)0x0B),
	    ERROR_SNSTART ((byte)0x0C),
	    ERROR_SNAPI ((byte)0x0D);

		private static final Map<Integer,EsnAPIBootloadResponse> lookup 
		= new HashMap<Integer,EsnAPIBootloadResponse>();

		static {
			for(EsnAPIBootloadResponse s : EnumSet.allOf(EsnAPIBootloadResponse.class))
				lookup.put(s.getCode(), s);
		}

		private int code;

		private EsnAPIBootloadResponse(int code) {
			this.code = code;
		}

		public int getCode() { return code; }

		public static EsnAPIBootloadResponse get(int code) { 
			return lookup.get(code); 
		}
	}
	
	/**
	 * Enumeration for device info
	 */
	public static enum EsnAPIInfoValues
	{
		ID ((byte)0x01),
		MANUFAC ((byte)0x02),
		PROFILE ((byte)0x03),
		REVISION ((byte)0x04);
		
		byte m_value = 0;
		EsnAPIInfoValues(byte b) {m_value = b;}
	}
}
