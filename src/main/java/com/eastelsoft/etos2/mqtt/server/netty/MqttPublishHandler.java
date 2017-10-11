package com.eastelsoft.etos2.mqtt.server.netty;

import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.List;
import java.util.Map;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.Topic;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttPublishHandler extends MqttMessageHandler<MqttPublishMessage> {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttPublishHandler.class);
	private final MqttServer mqttServer;
	
	public MqttPublishHandler(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}
	
	@Override
	public void onData(Client client, Map<String, List<String>> parameters,
			MqttPublishMessage data) throws Exception {
		// TODO Auto-generated method stub
		IntfLoggerUtil.logInboundMessage(logger,
				MqttMessageType.PUBLISH.name(), client, data.toString());
		final MqttQoS qos = data.fixedHeader().qosLevel();
		QosPublisher qosPublisher = QosPublisherFactory.makeQosPublisher(qos.value(), mqttServer);
		qosPublisher.publish(client, data);
	}
}
