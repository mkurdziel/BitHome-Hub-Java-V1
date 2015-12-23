package synet.controller.messaging.messages;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class MsgXbeeTxStatus extends Msg {
	private final static String TAG = "MsgXbeeTxStatus";
	
	/**
	 * Delivery Status
	 */
	public static enum EsnTxDeliveryStatus
	{
		SUCCESS (0x00),
		CCA_FAILURE (0x02),
		INVALID_DEST_ENDPOINT (0x15),
		NETWORK_ACK_FAILURE (0x21),
		NOT_JOINED_TO_NETWORK (0x22),
		SELF_ADDRESSED (0x23),
		ADDRESS_NOT_FOUND (0x24),
		ROUTE_NOT_FOUND (0x25);

		private static final Map<Integer,EsnTxDeliveryStatus> lookup 
		= new HashMap<Integer,EsnTxDeliveryStatus>();

		static {
			for(EsnTxDeliveryStatus s : EnumSet.allOf(EsnTxDeliveryStatus.class))
				lookup.put(s.getCode(), s);
		}

		private int code;

		private EsnTxDeliveryStatus(int code) {
			this.code = code;
		}

		public int getCode() { return code; }

		public static EsnTxDeliveryStatus get(int code) { 
			return lookup.get(code); 
		}
	}
	
	/**
	 * Discovery Status
	 */
	public static enum EsnTxDiscoveryStatus
	{
		NO_OVERHEAD (0x00),
		ADDRESS_DISCOVERY (0x01),
		ROUTE_DISCOVERY (0x02),
		ADDRESS_AND_ROUTE_DISCOVERY (0x03);

		private static final Map<Integer,EsnTxDiscoveryStatus> lookup 
		= new HashMap<Integer,EsnTxDiscoveryStatus>();

		static {
			for(EsnTxDiscoveryStatus s : EnumSet.allOf(EsnTxDiscoveryStatus.class))
				lookup.put(s.getCode(), s);
		}

		private int code;

		private EsnTxDiscoveryStatus(int code) {
			this.code = code;
		}

		public int getCode() { return code; }

		public static EsnTxDiscoveryStatus get(int code) { 
			return lookup.get(code); 
		}
	}
	
	
	private int m_frameId;
	private int m_retryCount;
	private EsnTxDeliveryStatus m_deliveryStatus;
	private EsnTxDiscoveryStatus m_discoveryStatus;
	
	/**
	 * @param p_frameId
	 * @param p_retryCount
	 * @param p_deliveryStatus
	 * @param p_discoveryStatus
	 */
	public MsgXbeeTxStatus(int p_frameId, int p_retryCount, int p_deliveryStatus, int p_discoveryStatus)
	{
		m_frameId = p_frameId;
		m_retryCount = p_retryCount;
		m_deliveryStatus = EsnTxDeliveryStatus.get(p_deliveryStatus);
		m_discoveryStatus = EsnTxDiscoveryStatus.get(p_discoveryStatus);
	}

	public int getFrameId() {
		return m_frameId;
	}

	public int getRetryCount() {
		return m_retryCount;
	}

	public EsnTxDeliveryStatus getDeliveryStatus() {
		return m_deliveryStatus;
	}

	public EsnTxDiscoveryStatus getDiscoveryStatus() {
		return m_discoveryStatus;
	}

	@Override
	// Internal message so no API is needed
	public byte getAPI() {
		return 0;
	}

	@Override
	public String getMsgType() {
		return TAG;
	}
	
	/* (non-Javadoc)
	 * @see synet.controller.messaging.messages.Msg#getDesc()
	 */
	public String getDescription()
	{
		return String.format("FrameId: %d, Retries: %d, Delivery:%s, Discovery:%s", 
				m_frameId, m_retryCount, m_deliveryStatus, m_discoveryStatus);
	}

}
