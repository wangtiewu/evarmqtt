package com.eastelsoft.etos2.mqtt.cluster;

public interface Store {

    void set(String key, Object val);

    <T> T get(String key);

    boolean has(String key);

    void del(String key);

}
