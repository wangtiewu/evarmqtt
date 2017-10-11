package com.eastelsoft.etos2.mqtt.server.netty;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttServer;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class QosNotDefPublisher extends QosPublisher {
	private static final Logger logger = LoggerFactory
			.getLogger(QosNotDefPublisher.class);

	protected QosNotDefPublisher(MqttServer mqttServer) {
		super(mqttServer);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean publish(Client client, MqttPublishMessage msg) {
		// TODO Auto-generated method stub
		final int messageId = MqttProtoHelper.messageId(msg);
		final MqttQoS qos = msg.fixedHeader().qosLevel();
		logger.error(
				"{Publish topic {} fail：qos {} is not valid，sessionId={}，identifier={}，remoteAddress={}, messageId={}",
				msg.variableHeader().topicName(), qos, client.getSessionId(),
				client.getSession().getIdentifier(), client.getRemoteAddress(),
				messageId);
		return false;
	}

}
