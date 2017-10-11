package com.eastelsoft.etos2.mqtt.cluster.memory;

import com.eastelsoft.etos2.mqtt.cluster.PubSubListener;
import com.eastelsoft.etos2.mqtt.cluster.PubSubStore;
import com.eastelsoft.etos2.mqtt.cluster.PubSubType;
import com.eastelsoft.etos2.mqtt.cluster.message.PubSubMessage;


public class MemoryPubSubStore implements PubSubStore {

    @Override
    public void publish(PubSubType type, PubSubMessage msg) {
    }

    @Override
    public <T extends PubSubMessage> void subscribe(PubSubType type, PubSubListener<T> listener, Class<T> clazz) {
    }

    @Override
    public void unsubscribe(PubSubType type) {
    }

    @Override
    public void shutdown() {
    }

}
