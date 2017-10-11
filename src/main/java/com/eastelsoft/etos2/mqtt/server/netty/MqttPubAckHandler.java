package com.eastelsoft.etos2.mqtt.server.netty;

import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

import java.util.List;
import java.util.Map;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttPubAckHandler extends MqttMessageHandler<MqttMessage> {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttPubAckHandler.class);
	private final MqttServer mqttServer;

	public MqttPubAckHandler(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}

	@Override
	public void onData(Client client, Map<String, List<String>> parameters,
			MqttMessage data) throws Exception {
		// TODO Auto-generated method stub
		IntfLoggerUtil.logInboundMessage(logger, MqttMessageType.PUBACK.name(),
				client, data.toString());
		final int messageId = MqttProtoHelper.messageId(data);
		StoredMessage pubMessage = client
				.removeOutboundFlightMessage(messageId);
		if (pubMessage != null) {
			logger.info("Publish topic {} acked：sessionId={},messageId={}，message={}",
					pubMessage.getTopic(), client.getSessionId(), messageId, pubMessage.toString());
		} else {
			logger.warn(
					"{} publish acked fail: messageId={}无对应的消息，identifier={}，remoteAddress={}",
					client.getSessionId(), messageId, client.getSession()
							.getIdentifier(), client.getRemoteAddress());
		}
	}
}
