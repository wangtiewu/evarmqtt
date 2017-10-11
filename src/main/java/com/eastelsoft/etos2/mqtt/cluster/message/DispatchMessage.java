package com.eastelsoft.etos2.mqtt.cluster.message;

import com.eastelsoft.etos2.mqtt.server.BaseMessage;

public class DispatchMessage extends PubSubMessage {

    private static final long serialVersionUID = 6692047718303934349L;

    private String room;
    private String namespace;
    private BaseMessage payload;

    public DispatchMessage() {
    }

    public DispatchMessage(String room, BaseMessage payload, String namespace) {
        this.room = room;
        this.payload = payload;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public BaseMessage getPayload() {
        return payload;
    }

    public String getRoom() {
        return room;
    }

}
