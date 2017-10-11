package com.eastelsoft.etos2.mqtt.cluster.message;

import java.util.UUID;


public class DisconnectMessage extends PubSubMessage {

    private static final long serialVersionUID = -2763553673397520368L;

    private UUID sessionId;

    public DisconnectMessage() {
    }

    public DisconnectMessage(UUID sessionId) {
        super();
        this.sessionId = sessionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

}
