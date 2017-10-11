package com.eastelsoft.etos2.mqtt.cluster.memory;

import io.netty.util.internal.PlatformDependent;

import java.util.Map;
import java.util.UUID;

import com.eastelsoft.etos2.mqtt.cluster.BaseStoreFactory;
import com.eastelsoft.etos2.mqtt.cluster.PubSubStore;
import com.eastelsoft.etos2.mqtt.cluster.Store;


public class MemoryStoreFactory extends BaseStoreFactory {

    private final MemoryPubSubStore pubSubMemoryStore = new MemoryPubSubStore();

    @Override
    public Store createStore(UUID sessionId) {
        return new MemoryStore();
    }

    @Override
    public PubSubStore pubSubStore() {
        return pubSubMemoryStore;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (local session store only)";
    }

    @Override
    public <K, V> Map<K, V> createMap(String name) {
        return PlatformDependent.newConcurrentHashMap();
    }

}
