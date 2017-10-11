package com.eastelsoft.etos2.mqtt.cluster.memory;

import io.netty.util.internal.PlatformDependent;

import java.util.Map;

import com.eastelsoft.etos2.mqtt.cluster.Store;

public class MemoryStore implements Store {

    private final Map<String, Object> store = PlatformDependent.newConcurrentHashMap();

    @Override
    public void set(String key, Object value) {
        store.put(key, value);
    }

    @Override
    public <T> T get(String key) {
        return (T) store.get(key);
    }

    @Override
    public boolean has(String key) {
        return store.containsKey(key);
    }

    @Override
    public void del(String key) {
        store.remove(key);
    }

}
