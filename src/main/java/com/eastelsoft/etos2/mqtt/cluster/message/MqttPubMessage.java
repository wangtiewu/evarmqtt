package com.eastelsoft.etos2.mqtt.cluster.message;

import com.eastelsoft.etos2.mqtt.server.StoredMessage;

public class MqttPubMessage extends PubSubMessage {

    private static final long serialVersionUID = 6692047718303934349L;

    private StoredMessage storedMessage;

    public MqttPubMessage() {
    }

    public MqttPubMessage(StoredMessage storedMessage) {
        this.storedMessage = storedMessage;
    }

	public StoredMessage getStoredMessage() {
		return storedMessage;
	}

	public void setStoredMessage(StoredMessage storedMessage) {
		this.storedMessage = storedMessage;
	}
}
