package com.eastelsoft.etos2.mqtt.server.netty;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.RetainMessageStore;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.server.Topic;

import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

public class Qos0Publisher extends QosPublisher {
	private static final Logger logger = LoggerFactory
			.getLogger(Qos0Publisher.class);
	private final RetainMessageStore retainMessageStore = (RetainMessageStore) EvarBeanFactory.instance().makeBean(
			"retainMessageStore");
	
	public Qos0Publisher(MqttServer mqttServer) {
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
		if (msg.fixedHeader().isRetain()) {
			// QoS == 0 && retain => clean old retained
			retainMessageStore.remove(topic);
			logger.info(
					"Delete retain message: topic={}，sessionId={}，identifier={}，remoteAddress={}，messageId={}",
					topic, client.getSessionId(), client.getSession()
							.getIdentifier(), client.getRemoteAddress(),
					messageId);
		}
		StoredMessage storedMsg = new StoredMessage(topic,
				readBytesAndRewind(msg.payload()), msg.fixedHeader().isRetain(), msg.fixedHeader()
						.qosLevel().value(), mqttServer.getServerId());
		publish2Subscribers(storedMsg);
		return true;
	}
}
