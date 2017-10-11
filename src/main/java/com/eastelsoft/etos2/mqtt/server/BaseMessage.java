package com.eastelsoft.etos2.mqtt.server;

import java.io.Serializable;

public abstract class BaseMessage implements Serializable{
	public enum OperFlag {
		ADD("add"),DEL("del");
		private String value;

		public String value() {
			return value;
		}

		private OperFlag(String value) {
			this.value = value;
		}

		public static OperFlag from(String value) {
			if ("add".equals(value)) {
				return ADD;
			}
			if ("del".equals(value)) {
				return DEL;
			}
			return null;
		}

		public static OperFlag valueOf(int value) {
			switch (value) {
			case 0:
				return ADD;
			case 1:
				return DEL;
			default:
				return null;
			}
		}

		public static String enumsToString() {
			StringBuffer sb = new StringBuffer();
			for (OperFlag enum2 : OperFlag.values()) {
				sb.append(enum2).append("(").append(enum2.value).append(")")
						.append(",");
			}
			return sb.toString().length() > 0 ? sb.toString().substring(0,
					sb.toString().length() - 1) : sb.toString();
		}
	};
}
