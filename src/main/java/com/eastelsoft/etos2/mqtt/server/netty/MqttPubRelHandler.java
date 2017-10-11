package com.eastelsoft.etos2.mqtt.server.netty;

import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

import java.util.List;
import java.util.Map;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.DataListener;
import com.eastelsoft.etos2.mqtt.server.MqttConsts;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttPubRelHandler extends MqttMessageHandler<MqttMessage> {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttPubRelHandler.class);
	private final MqttServer mqttServer;

	public MqttPubRelHandler(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}

	@Override
	public void onData(Client client, Map<String, List<String>> parameters,
			MqttMessage data) throws Exception {
		// TODO Auto-generated method stub
		IntfLoggerUtil.logInboundMessage(logger, MqttMessageType.PUBREL.name(),
				client, data.toString());
		final int messageId = MqttProtoHelper.messageId(data);
		MqttMessage pubComp = makePubComp(messageId);
		client.send(MqttMessageType.PUBCOMP.name(), pubComp);
		IntfLoggerUtil.logOutboundMessage(logger,
				MqttMessageType.PUBCOMP.name(), client, pubComp.toString());
		// publish2Subscribers
		StoredMessage storedMessage = client.removeInboundFlightMessage(messageId);
		if (storedMessage == null) {
			logger.error(
					"{Publish fail：messageId={} 对应的消息不存在，sessionId={}，identifier={}，remoteAddress={}",
					messageId, client.getSessionId(), client.getSession()
							.getIdentifier(), client.getRemoteAddress());
		} else {
			QosPublisherFactory.makeQosPublisher(storedMessage.getQos(), mqttServer).publish2Subscribers(storedMessage);
		}
	}

	private MqttMessage makePubComp(int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PUBCOMP, false, AT_MOST_ONCE, false, 0);
		MqttMessage pubCompMessage = new MqttMessage(fixedHeader,
				from(messageId));
		return pubCompMessage;
	}
}
