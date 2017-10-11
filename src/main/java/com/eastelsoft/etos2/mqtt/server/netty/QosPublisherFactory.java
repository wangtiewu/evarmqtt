package com.eastelsoft.etos2.mqtt.server.netty;

import com.eastelsoft.etos2.mqtt.server.MqttServer;

public class QosPublisherFactory {
	private static QosPublisher qos0Publisher;
	private static QosPublisher qos1Publisher;
	private static QosPublisher qos2Publisher;
	private static QosPublisher qosNotDefPublisher;

	public static QosPublisher makeQosPublisher(int qos, MqttServer mqttServer) {
		switch (qos) {
		case 0:
			if (qos0Publisher == null) {
				synchronized (QosPublisherFactory.class) {
					if (qos0Publisher == null) {
						qos0Publisher = new Qos0Publisher(mqttServer);
					}
				}
			}
			return qos0Publisher;
		case 1:
			if (qos1Publisher == null) {
				synchronized (QosPublisherFactory.class) {
					if (qos1Publisher == null) {
						qos1Publisher = new Qos1Publisher(mqttServer);
					}
				}
			}
			return qos1Publisher;
		case 2:
			if (qos2Publisher == null) {
				synchronized (QosPublisherFactory.class) {
					if (qos2Publisher == null) {
						qos2Publisher = new Qos2Publisher(mqttServer);
					}
				}
			}
			return qos2Publisher;
		default:
			if (qosNotDefPublisher == null) {
				synchronized (QosPublisherFactory.class) {
					if (qosNotDefPublisher == null) {
						qosNotDefPublisher = new QosNotDefPublisher(mqttServer);
					}
				}
			}
			return qosNotDefPublisher;
		}
	}
}
