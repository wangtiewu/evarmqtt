package com.eastelsoft.etos2.mqtt.cluster.message;

import java.util.UUID;

public class ConnectMessage extends PubSubMessage {

    private static final long serialVersionUID = 3108918714495865101L;

    private UUID sessionId;

    public ConnectMessage() {
    }

    public ConnectMessage(UUID sessionId) {
        super();
        this.sessionId = sessionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

}
