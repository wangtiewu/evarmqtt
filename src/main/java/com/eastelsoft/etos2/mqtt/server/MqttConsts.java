package com.eastelsoft.etos2.mqtt.server;

/**
 * 
 * @author eastelsoft
 *
 */
public class MqttConsts {
	public enum QoS {
		AT_MOST_ONCE(0), AT_LEAST_ONCE(1), EXACTLY_ONCE(2), FAILURE(0x80);

		private final int value;

		QoS(int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}

		public static QoS valueOf(int value) {
			for (QoS q : values()) {
				if (q.value == value) {
					return q;
				}
			}
			throw new IllegalArgumentException("invalid QoS: " + value);
		}
	}

	/**
	 * 等待时间，单位秒
	 *
	 */
	public enum WaitTimeOut {
		QOS2_PUBREL_TIMEOUT(3600), QOS2_PUBREC_TIMEOUT(30), QOS1_PUBACK_TIMEOUT(
				30), QOS2_PUBCOMP_TIMEOUT(30);

		private final int value;

		WaitTimeOut(int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}

		public static WaitTimeOut valueOf(int value) {
			for (WaitTimeOut q : values()) {
				if (q.value == value) {
					return q;
				}
			}
			throw new IllegalArgumentException("invalid WaitTimeOut: " + value);
		}
	}

	public static final long SESSION_TTL_TIMES = 100;//

}
