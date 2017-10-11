package com.eastelsoft.etos2.mqtt.server.netty;

import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttConsts;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;
import com.eastelsoft.etos2.mqtt.util.SchedulerKey;
import com.eastelsoft.etos2.mqtt.util.SchedulerKey.Type;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttPubRecHandler extends MqttMessageHandler<MqttMessage> {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttPubRecHandler.class);
	private final MqttServer mqttServer;

	public MqttPubRecHandler(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}

	@Override
	public void onData(Client client, Map<String, List<String>> parameters,
			MqttMessage data) throws Exception {
		// TODO Auto-generated method stub
		IntfLoggerUtil.logInboundMessage(logger, MqttMessageType.PUBREC.name(),
				client, data.toString());
		final int messageId = MqttProtoHelper.messageId(data);
		MqttMessage pubRel = makePubRel(messageId);
		sendPubRel(client, messageId, pubRel);
		StoredMessage pubMessage = client
				.removeOutboundFlightMessage(messageId);
		if (pubMessage != null) {
			logger.info(
					"Publish topic {} received：sessionId={},messageId={}，message={}",
					pubMessage.getTopic(), client.getSessionId(), messageId,
					pubMessage.toString());
		} else {
			logger.warn(
					"{} publish received fail: messageId={}无对应的消息，identifier={}，remoteAddress={}",
					client.getSessionId(), messageId, client.getSession()
							.getIdentifier(), client.getRemoteAddress());
		}
	}

	private void sendPubRel(Client client, int messageId, MqttMessage pubRel) {
		mqttServer.getScheduler().schedule(
				new SchedulerKey(SchedulerKey.Type.QOS2_WAIT_PUBCOMP,
						client.getSessionId() + ":" + messageId),
				new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						// resend pubRel
						if (!client.isDestroyed() && client.isTransportActive()) {
							sendPubRel(client, messageId, pubRel);
						}
					}
				}, MqttConsts.WaitTimeOut.QOS2_PUBCOMP_TIMEOUT.value(),
				TimeUnit.SECONDS);
		client.send(MqttMessageType.PUBREL.name(), pubRel);
		IntfLoggerUtil.logOutboundMessage(logger,
				MqttMessageType.PUBREL.name(), client, pubRel.toString());
	}

	private MqttMessage makePubRel(int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PUBREL, false, AT_MOST_ONCE, false, 0);
		MqttMessage pubRelMessage = new MqttMessage(fixedHeader,
				from(messageId));
		return pubRelMessage;
	}
}
