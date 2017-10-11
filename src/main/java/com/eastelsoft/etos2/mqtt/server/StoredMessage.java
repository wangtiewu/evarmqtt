package com.eastelsoft.etos2.mqtt.server;

import java.io.Serializable;

import eet.evar.StringDeal;

public class StoredMessage implements Serializable {
	private Topic topic;
	private byte[] payload;
	private boolean retain;
	private int qos;
	private String source;

	public StoredMessage() {

	}

	public StoredMessage(StoredMessage orig) {
		this.topic = orig.topic;
		this.payload = orig.payload;
		this.retain = orig.retain;
		this.qos = orig.qos;
		this.source = orig.source;
	}

	public StoredMessage(Topic topic, byte[] payload, boolean retain, int qos,
			String source) {
		// TODO Auto-generated constructor stub
		this.topic = topic;
		this.payload = payload;
		this.retain = retain;
		this.qos = qos;
		this.source = source;
	}

	public Topic getTopic() {
		return topic;
	}

	public void setTopic(Topic topic) {
		this.topic = topic;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public boolean isRetain() {
		return retain;
	}

	public void setRetain(boolean retain) {
		this.retain = retain;
	}

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		StoredMessage that = (StoredMessage) o;
		if (payload != null ? !payload.equals(that.payload)
				: that.payload != null)
			return false;
		return !(topic != null ? !topic.equals(that.topic) : that.topic != null);
	}

	@Override
	public int hashCode() {
		int result = payload != null ? payload.hashCode() : 0;
		result = 31 * result + (topic != null ? topic.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return StringDeal.format(
				"[filter: {}, qos: {}, retain: {}，payload: {}, source: {}]",
				this.topic, this.qos, this.retain, new String(this.payload),
				this.source);
	}
}
