package com.eastelsoft.etos2.mqtt.util;

public class SchedulerKey {

	public enum Type {
		DEFAULT, LOGIN_TIMEOUT, PING_TIMEOUT, QOS2_WAIT_PUBREL, QOS1_WAIT_PUBACK, QOS2_WAIT_PUBREC, QOS2_WAIT_PUBCOMP
	};

	private final Type type;
	private final String keyId;

	public SchedulerKey(Type type, String keyId) {
		this.type = type;
		this.keyId = keyId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((keyId == null) ? 0 : keyId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SchedulerKey other = (SchedulerKey) obj;
		if (keyId == null) {
			if (other.keyId != null)
				return false;
		} else if (!keyId.equals(other.keyId))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

}
