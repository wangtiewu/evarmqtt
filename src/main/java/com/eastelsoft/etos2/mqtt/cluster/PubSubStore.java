package com.eastelsoft.etos2.mqtt.cluster;

import com.eastelsoft.etos2.mqtt.cluster.message.PubSubMessage;

public interface PubSubStore {

    void publish(PubSubType type, PubSubMessage msg);

    <T extends PubSubMessage> void subscribe(PubSubType type, PubSubListener<T> listener, Class<T> clazz);

    void unsubscribe(PubSubType type);

    void shutdown();

}
