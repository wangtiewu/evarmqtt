package com.eastelsoft.etos2.mqtt.cluster;

import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;

public interface MqttNotifier {
	void publish(StoredMessage storedMessage, String... identifiers);

	void publish(StoredMessage storedMessage);

	void disconnect(String sessionId);

	MqttServer getMqttServer();

	void setMqttServer(MqttServer mqttServer);
}
