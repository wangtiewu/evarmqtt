package com.eastelsoft.etos2.mqtt.server.netty;

import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.FAILURE;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eastelsoft.etos2.mqtt.auth.AuthService;
import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.RetainMessageStore;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.server.Subscription;
import com.eastelsoft.etos2.mqtt.server.SubscriptionStore;
import com.eastelsoft.etos2.mqtt.server.Topic;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;

import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttSubscribeHandler extends
		MqttMessageHandler<MqttSubscribeMessage> {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttSubscribeHandler.class);
	private final AuthService authService = (AuthService) EvarBeanFactory
			.instance().makeBean("authService");
	private final RetainMessageStore retainMessageStore = (RetainMessageStore) EvarBeanFactory
			.instance().makeBean("retainMessageStore");
	private final SubscriptionStore subscriptionStore = (SubscriptionStore) EvarBeanFactory
			.instance().makeBean("subscriptionStore");
	private final MqttServer mqttServer;

	public MqttSubscribeHandler(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}

	@Override
	public void onData(Client client, Map<String, List<String>> parameters,
			MqttSubscribeMessage data) throws Exception {
		// TODO Auto-generated method stub
		IntfLoggerUtil.logInboundMessage(logger,
				MqttMessageType.SUBSCRIBE.name(), client, data.toString());
		final MqttQoS qos = data.fixedHeader().qosLevel();
		final int messageId = MqttProtoHelper.messageId(data);
		List<MqttTopicSubscription> subAcks = new ArrayList<MqttTopicSubscription>();
		Map<Topic, MqttQoS> validTopics = new HashMap<Topic, MqttQoS>();
		for (MqttTopicSubscription topicSub : data.payload()
				.topicSubscriptions()) {
			Topic topic = new Topic(topicSub.topicName());
			if (!topic.isValid()) {
				logger.error(
						"{Subscribe topic {} fail：topic is not valid，sessionId={}，identifier={}，remoteAddress={}, messageId={}",
						topicSub.topicName(), client.getSessionId(), client
								.getSession().getIdentifier(), client
								.getRemoteAddress(), messageId);
				subAcks.add(new MqttTopicSubscription(topicSub.topicName(),
						FAILURE));
			} else {
				if (!authService
						.canSub(client.getSession().getUserName(), client
								.getSession().getIdentifier(), topic, qos
								.value())) {
					logger.error(
							"{Subscribe topic {} fail：have no permissions，sessionId={}，identifier={}，remoteAddress={}，messageId={}",
							topicSub.topicName(), client.getSessionId(), client
									.getSession().getIdentifier(), client
									.getRemoteAddress(), messageId);
					subAcks.add(new MqttTopicSubscription(topicSub.topicName(),
							FAILURE));
				} else {
					subAcks.add(new MqttTopicSubscription(topicSub.topicName(),
							topicSub.qualityOfService()));
					validTopics.put(topic, topicSub.qualityOfService());
				}
			}
		}
		List<Integer> grantedQoSLevels = new ArrayList<>();
		for (MqttTopicSubscription subscription : subAcks) {
			grantedQoSLevels.add(subscription.qualityOfService().value());
		}
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.SUBACK, false, AT_LEAST_ONCE, false, 0);
		MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoSLevels);
		MqttSubAckMessage subAckMessage = new MqttSubAckMessage(fixedHeader,
				from(messageId), payload);
		client.send(MqttMessageType.SUBACK.name(), subAckMessage);
		IntfLoggerUtil
				.logOutboundMessage(logger, MqttMessageType.SUBACK.name(),
						client, subAckMessage.toString());
		// save subscribe topics
		for (Map.Entry<Topic, MqttQoS> entry : validTopics.entrySet()) {
			Subscription subscription = new Subscription(client.getSessionId(), client.getSession()
					.getIdentifier(), entry.getKey(), entry.getValue().value(),
					mqttServer.getServerId());
			subscriptionStore.sub(subscription);
			client.sub(subscription);
		}
		// fire retain messages: retail=1,qos=min(subQoS,retainMsgQoS)
		for (Map.Entry<Topic, MqttQoS> entry : validTopics.entrySet()) {
			List<StoredMessage> retainMessages = retainMessageStore.match(entry
					.getKey());
			if (retainMessages == null || retainMessages.isEmpty()) {
				continue;
			}
			for (StoredMessage retainMessage : retainMessages) {
				StoredMessage pubMessage = new StoredMessage(retainMessage);
				pubMessage.setTopic(entry.getKey());
				pubMessage.setQos(Math.min(pubMessage.getQos(), entry
						.getValue().value()));
				int pubMessageId = client.createMessageId();
				QosPublisherFactory.makeQosPublisher(pubMessage.getQos(),
						mqttServer).publish2Subscriber(client, pubMessageId,
						pubMessage);
			}
		}
	}

	private MqttPublishMessage makePublishMessage(int messageId,
			StoredMessage storedMessage) {
		// TODO wtw ByteBuf使用缓存
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PUBLISH, false, MqttQoS.valueOf(storedMessage
						.getQos()), storedMessage.isRetain(), 0);
		MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(
				storedMessage.getTopic().getTopicFilter(), messageId);
		return new MqttPublishMessage(fixedHeader, varHeader,
				Unpooled.wrappedBuffer(storedMessage.getPayload()));
	}

}
