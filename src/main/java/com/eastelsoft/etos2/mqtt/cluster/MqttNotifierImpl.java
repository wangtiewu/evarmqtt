package com.eastelsoft.etos2.mqtt.cluster;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.Client.ConnectionCloseType;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.Namespace;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.server.netty.QosPublisher;
import com.eastelsoft.etos2.mqtt.server.netty.QosPublisherFactory;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttNotifierImpl implements MqttNotifier {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttNotifierImpl.class);
	private MqttServer mqttServer;

	@Override
	public void publish(StoredMessage storedMessage, String... identifiers) {
		// TODO Auto-generated method stub
		QosPublisher qosPublisher = QosPublisherFactory.makeQosPublisher(
				storedMessage.getQos(), mqttServer);
		for (String identifier : identifiers) {
			qosPublisher.publish2Subscriber(identifier, storedMessage);
		}
	}

	@Override
	public void publish(StoredMessage storedMessage) {
		// TODO Auto-generated method stub
		QosPublisher qosPublisher = QosPublisherFactory.makeQosPublisher(
				storedMessage.getQos(), mqttServer);
		qosPublisher.publish2Subscribers(storedMessage);
	}


	@Override
	public void disconnect(String sessionId) {
		// TODO Auto-generated method stub
		Namespace mqttNamespace = mqttServer
				.getNamespace(MqttServer.NAMESPACE_MQTT);
		Client client = mqttNamespace.getClient(sessionId);
		if (client != null && !client.isDestroyed()) {
			client.assignConnCloseType(ConnectionCloseType.DUP_CONNECT);
			client.disconnect();
			logger.info("{} 踢下线：dentifier={}，remoteAddress={}", sessionId,
					client.getSession() == null ? "" : client.getSession()
							.getIdentifier(), client.getSession() == null ? ""
							: client.getSession().getRemoteAddress());
		}
	}

	public MqttServer getMqttServer() {
		return mqttServer;
	}

	public void setMqttServer(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}

}
