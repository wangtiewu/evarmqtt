package com.eastelsoft.etos2.mqtt.server;

import java.io.Serializable;

import eet.evar.StringDeal;

public final class Subscription implements Serializable {
	private static final long serialVersionUID = -3383457629635732794L;
	final String sessionId;
	final String identifier;
	final Topic topic;
	final int qos;
	final String serverId;

	public Subscription() {
		this(null, null, null, 0, null);
	}

	public Subscription(String sessionId, String identifier, Topic topic,
			int qos, String serverId) {
		this.sessionId = sessionId;
		this.identifier = identifier;
		this.topic = topic;
		this.qos = qos;
		this.serverId = serverId;
	}

	public Subscription(Subscription orig) {
		this.sessionId = orig.sessionId;
		this.identifier = orig.identifier;
		this.topic = orig.topic;
		this.qos = orig.qos;
		this.serverId = orig.serverId;
	}

	public String getIdentifier() {
		return identifier;
	}

	public Topic getTopic() {
		return topic;
	}

	public int getQos() {
		return qos;
	}

	public String getServerId() {
		return serverId;
	}

	public String getSessionId() {
		return sessionId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Subscription that = (Subscription) o;
		if (identifier != null ? !identifier.equals(that.identifier)
				: that.identifier != null) {
			return false;
		}
		if (serverId != null ? !serverId.equals(that.serverId)
				: that.serverId != null) {
			return false;
		}
		if (sessionId != null ? !sessionId.equals(that.sessionId)
				: that.sessionId != null) {
			return false;
		}
		return !(topic != null ? !topic.equals(that.topic) : that.topic != null);
	}

	@Override
	public int hashCode() {
		int result = identifier != null ? identifier.hashCode() : 0;
		result = 31 * result + (topic != null ? topic.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return StringDeal
				.format("[filter: {}, qos: {}, identifier: {}, sessionId: {}, serverId: {}]",
						this.topic, this.qos, this.identifier, this.sessionId,
						this.serverId);
	}

	@Override
	public Subscription clone() {
		try {
			return (Subscription) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
