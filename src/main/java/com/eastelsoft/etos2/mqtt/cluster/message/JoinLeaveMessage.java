package com.eastelsoft.etos2.mqtt.cluster.message;

import java.util.UUID;

public class JoinLeaveMessage extends PubSubMessage {

    private static final long serialVersionUID = -944515928988033174L;

    private String sessionId;
    private String namespace;
    private String room;

    public JoinLeaveMessage() {
    }

    public JoinLeaveMessage(String id, String room, String namespace) {
        super();
        this.sessionId = id;
        this.room = room;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRoom() {
        return room;
    }

}
