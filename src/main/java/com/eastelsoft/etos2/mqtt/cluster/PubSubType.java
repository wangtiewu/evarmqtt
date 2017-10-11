package com.eastelsoft.etos2.mqtt.cluster;

public enum PubSubType {

    CONNECT, DISCONNECT, JOIN, LEAVE, DISPATCH, MQTT_PUB;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

}
