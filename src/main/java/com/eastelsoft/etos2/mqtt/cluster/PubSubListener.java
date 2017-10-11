package com.eastelsoft.etos2.mqtt.cluster;

public interface PubSubListener<T> {

    void onMessage(T data);

}
