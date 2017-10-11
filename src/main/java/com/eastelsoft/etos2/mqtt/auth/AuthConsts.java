package com.eastelsoft.etos2.mqtt.auth;

public class AuthConsts {
	// 能力名称定义 //
	public enum CAPABILITY {
		SMS(1), MMS(2), LBS(3), VOS(4), MQTT(10);
		private int value;

		public int value() {
			return value;
		}

		private CAPABILITY(int value) {
			this.value = value;
		}

		public static CAPABILITY from(String value) {
			if ("SMS".equals(value)) {
				return SMS;
			}
			if ("SMS".equals(value)) {
				return MMS;
			}
			if ("LBS".equals(value)) {
				return LBS;
			}
			if ("VOS".equals(value)) {
				return VOS;
			}
			if ("MQTT".equals(value)) {
				return MQTT;
			}
			return null;
		}

		public static CAPABILITY valueOf(int value) {
			switch (value) {
			case 1:
				return SMS;
			case 2:
				return MMS;
			case 3:
				return LBS;
			case 4:
				return VOS;
			default:
				return null;
			}
		}

		public static String enumsToString() {
			StringBuffer sb = new StringBuffer();
			for (CAPABILITY enum2 : CAPABILITY.values()) {
				sb.append(enum2).append("(").append(enum2.value).append(")")
						.append(",");
			}
			return sb.toString().length() > 0 ? sb.toString().substring(0,
					sb.toString().length() - 1) : sb.toString();
		}
	};

	// 用户状态
	public enum USER_STATUS {
		NOMARL(1), STOPED(2);
		private int value;

		public int value() {
			return value;
		}

		private USER_STATUS(int value) {
			this.value = value;
		}

		public static USER_STATUS from(String value) {
			if ("NOMARL".equals(value)) {
				return NOMARL;
			}
			if ("STOPED".equals(value)) {
				return STOPED;
			}
			return STOPED;
		}

		public static USER_STATUS valueOf(int value) {
			switch (value) {
			case 1:
				return NOMARL;
			case 2:
				return STOPED;
			default:
				return null;
			}
		}

		public static String enumsToString() {
			StringBuffer sb = new StringBuffer();
			for (USER_STATUS enum2 : USER_STATUS.values()) {
				sb.append(enum2).append("(").append(enum2.value).append(")")
						.append(",");
			}
			return sb.toString().length() > 0 ? sb.toString().substring(0,
					sb.toString().length() - 1) : sb.toString();
		}
	};

	public enum QUOTA_PERIOD {
		ONCE(1), DAY(2), MONTH(3), YEAR(4);
		private int value;

		public int value() {
			return value;
		}

		private QUOTA_PERIOD(int value) {
			this.value = value;
		}

		public static QUOTA_PERIOD from(String value) {
			if ("ONCE".equals(value)) {
				return ONCE;
			}
			if ("DAY".equals(value)) {
				return DAY;
			}
			if ("MONTH".equals(value)) {
				return MONTH;
			}
			if ("YEAR".equals(value)) {
				return YEAR;
			}
			return ONCE;
		}

		public static QUOTA_PERIOD valueOf(int value) {
			switch (value) {
			case 1:
				return ONCE;
			case 2:
				return DAY;
			case 3:
				return MONTH;
			case 4:
				return YEAR;
			default:
				return null;
			}
		}

		public static String enumsToString() {
			StringBuffer sb = new StringBuffer();
			for (QUOTA_PERIOD enum2 : QUOTA_PERIOD.values()) {
				sb.append(enum2).append("(").append(enum2.value).append(")")
						.append(",");
			}
			return sb.toString().length() > 0 ? sb.toString().substring(0,
					sb.toString().length() - 1) : sb.toString();
		}
	};

	// 应用key类型
	public enum APP_KEY_TYPE {
		PLAIN(1), // 明文key
		MD5(2);// MD5(明文key)
		private int value;

		public int value() {
			return value;
		}

		private APP_KEY_TYPE(int value) {
			this.value = value;
		}

		public static APP_KEY_TYPE from(String value) {
			if ("PLAIN".equals(value)) {
				return PLAIN;
			}
			if ("MD5".equals(value)) {
				return MD5;
			}
			return null;
		}

		public static APP_KEY_TYPE valueOf(int value) {
			switch (value) {
			case 1:
				return PLAIN;
			case 2:
				return MD5;
			default:
				return null;
			}
		}

		public static String enumsToString() {
			StringBuffer sb = new StringBuffer();
			for (APP_KEY_TYPE enum2 : APP_KEY_TYPE.values()) {
				sb.append(enum2).append("(").append(enum2.value).append(")")
						.append(",");
			}
			return sb.toString().length() > 0 ? sb.toString().substring(0,
					sb.toString().length() - 1) : sb.toString();
		}
	};

	// http认证参数
	public enum HttpParamName {
		APP_ID("appId"), APP_KEY("appKey"), TIMESTAMP("timestamp");

		private String value;

		public String value() {
			return value;
		}

		private HttpParamName(String value) {
			this.value = value;
		}

		public static String enumsToString() {
			StringBuffer sb = new StringBuffer();
			for (HttpParamName enum2 : HttpParamName.values()) {
				sb.append(enum2).append("(").append(enum2.value).append(")")
						.append(",");
			}
			return sb.toString().length() > 0 ? sb.toString().substring(0,
					sb.toString().length() - 1) : sb.toString();
		}
	};

	private final static String REDIS_KEY_PRE_TOKEN = "token:";// token

	/**
	 * 生成Token key值
	 * 
	 * @param token
	 * @return
	 */
	public static String buildTokenKey(String md5AccessToken) {
		return REDIS_KEY_PRE_TOKEN + md5AccessToken;
	}
}
