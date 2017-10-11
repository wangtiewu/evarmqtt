package com.eastelsoft.etos2.mqtt.server;

import java.util.List;
import java.util.Set;

public interface SubscriptionStore {
	void sub(Subscription subscription);

	void sub(Set<Subscription> subscriptions);

	void unsub(Subscription subscription);

	void unsub(Set<Subscription> subscriptions);

	List<Subscription> match(Topic topic);

	void setMqttServer(MqttServer mqttServer);
}
