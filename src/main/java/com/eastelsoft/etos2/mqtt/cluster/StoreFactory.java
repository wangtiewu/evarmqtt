package com.eastelsoft.etos2.mqtt.cluster;

import java.util.Map;
import java.util.UUID;

import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.NamespacesHub;
import com.eastelsoft.etos2.mqtt.util.JsonSupport;


/**
 *
 * Creates a client Store and PubSubStore
 *
 */
public interface StoreFactory {

    PubSubStore pubSubStore();

    <K, V> Map<K, V> createMap(String name);

    Store createStore(UUID sessionId);

    void init(MqttServer mqttServer, NamespacesHub namespacesHub, JsonSupport jsonSupport);

    void shutdown();

}
