package com.eastelsoft.etos2.mqtt.server.netty;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

import java.util.List;
import java.util.Map;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;
import com.eastelsoft.etos2.mqtt.util.SchedulerKey;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttPubCompHandler extends MqttMessageHandler<MqttMessage> {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttPubCompHandler.class);
	private final MqttServer mqttServer;

	public MqttPubCompHandler(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}

	@Override
	public void onData(Client client, Map<String, List<String>> parameters,
			MqttMessage data) throws Exception {
		// TODO Auto-generated method stub
		IntfLoggerUtil.logInboundMessage(logger,
				MqttMessageType.PUBCOMP.name(), client, data.toString());
		final int messageId = MqttProtoHelper.messageId(data);
		mqttServer.getScheduler().cancel(
				new SchedulerKey(SchedulerKey.Type.QOS2_WAIT_PUBCOMP, client
						.getSessionId() + ":" + messageId));
	}
}
