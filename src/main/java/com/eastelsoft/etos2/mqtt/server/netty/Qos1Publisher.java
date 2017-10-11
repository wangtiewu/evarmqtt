package com.eastelsoft.etos2.mqtt.server.netty;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.RetainMessageStore;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.server.Topic;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;

import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;

public class Qos1Publisher extends QosPublisher {
	private static final Logger logger = LoggerFactory
			.getLogger(Qos1Publisher.class);
	private final RetainMessageStore retainMessageStore = (RetainMessageStore) EvarBeanFactory
			.instance().makeBean("retainMessageStore");

	public Qos1Publisher(MqttServer mqttServer) {
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
		if (!canPublish(client, topic, msg.fixedHeader().qosLevel())) {
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
		if (msg.fixedHeader().isRetain()) {
			if (!msg.payload().isReadable()) {
				retainMessageStore.remove(topic);
				logger.info(
						"Delete retain message: topic={}，sessionId={}，identifier={}，remoteAddress={}，messageId={}",
						topic, client.getSessionId(), client.getSession()
								.getIdentifier(), client.getRemoteAddress(),
						messageId);
			} else {
				retainMessageStore.put(topic,
						storedMsg);
			}
		}
		publish2Subscribers(storedMsg);
		MqttPubAckMessage pubAck = makePubAck(messageId);
		client.send(MqttMessageType.PUBACK.name(), pubAck);
		IntfLoggerUtil.logOutboundMessage(logger,
				MqttMessageType.PUBACK.name(), client, pubAck.toString());
		return true;
	}

	private MqttPubAckMessage makePubAck(int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader,
				from(messageId));
		return pubAckMessage;
	}
}
