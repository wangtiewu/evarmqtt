package com.eastelsoft.etos2.mqtt.server.netty;

import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;

import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttConsts;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.server.Topic;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;
import com.eastelsoft.etos2.mqtt.util.SchedulerKey;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class Qos2Publisher extends QosPublisher {
	private static final Logger logger = LoggerFactory
			.getLogger(Qos2Publisher.class);

	public Qos2Publisher(MqttServer mqttServer) {
		super(mqttServer);
	}

	public boolean publish(Client client, MqttPublishMessage msg) {
		// verify if topic can be write
		final Topic topic = new Topic(msg.variableHeader().topicName());
		final int messageId = MqttProtoHelper.messageId(msg);
		if (!topic.isValid()) {
			logger.error(
					"{Publish topic {} fail：topic is not valid，sessionId={}，identifier={}，remoteAddress={}, messageId={}",
					topic, client.getSessionId(), client.getSession()
							.getIdentifier(), client.getRemoteAddress(),
					messageId);
			return false;
		} else if (topic.containWildcard()) {
			logger.error(
					"{Publish topic {} fail：pushish topic can't contain wildcard，sessionId={}，identifier={}，remoteAddress={}, messageId={}",
					topic, client.getSessionId(), client.getSession()
							.getIdentifier(), client.getRemoteAddress(),
					messageId);
			return false;
		}
		if (!canPublish(client, topic, MqttQoS.AT_MOST_ONCE)) {
			logger.error(
					"{Publish topic {} fail：have no permissions，sessionId={}，identifier={}，remoteAddress={}，messageId={}",
					topic, client.getSessionId(), client.getSession()
							.getIdentifier(), client.getRemoteAddress(),
					messageId);
			return false;
		}
		if (!canPublish(client, topic, MqttQoS.AT_MOST_ONCE)) {
			logger.error(
					"{Publish topic {} fail：have no permissions，sessionId={}，identifier={}，remoteAddress={}，messageId={}",
					topic, client.getSessionId(), client.getSession()
							.getIdentifier(), client.getRemoteAddress(),
					messageId);
			return false;
		}
		StoredMessage storedMsg = new StoredMessage(topic,
				readBytesAndRewind(msg.payload()),
				msg.fixedHeader().isRetain(), msg.fixedHeader().qosLevel()
						.value(), mqttServer.getServerId());
		client.addInboundFlightMessage(messageId, storedMsg);
		MqttMessage pubReceived = makePubReceived(messageId);
		client.send(MqttMessageType.PUBREC.name(), pubReceived);
		IntfLoggerUtil.logOutboundMessage(logger,
				MqttMessageType.PUBREC.name(), client, pubReceived.toString());
		return true;
	}

	private MqttMessage makePubReceived(int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MqttMessage pubReceived = new MqttMessage(fixedHeader, from(messageId));
		return pubReceived;
	}
}
