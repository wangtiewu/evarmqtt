package com.eastelsoft.etos2.mqtt.server.netty;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;

import com.eastelsoft.etos2.mqtt.MqttConsts.ClusterType;
import com.eastelsoft.etos2.mqtt.MqttTaskExecutor;
import com.eastelsoft.etos2.mqtt.auth.AuthService;
import com.eastelsoft.etos2.mqtt.cluster.MqttNotifierHelper;
import com.eastelsoft.etos2.mqtt.cluster.PubSubType;
import com.eastelsoft.etos2.mqtt.cluster.message.MqttPubMessage;
import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.GlobalTopticFilterSubStore;
import com.eastelsoft.etos2.mqtt.server.MqttConsts;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.Namespace;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.server.Subscription;
import com.eastelsoft.etos2.mqtt.server.SubscriptionStore;
import com.eastelsoft.etos2.mqtt.server.Topic;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;
import com.eastelsoft.etos2.mqtt.util.SchedulerKey;

import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public abstract class QosPublisher {
	private static final Logger logger = LoggerFactory
			.getLogger(QosPublisher.class);
	private AuthService authService = (AuthService) EvarBeanFactory.instance()
			.makeBean("authService");
	private SubscriptionStore subscriptionStore = (SubscriptionStore) EvarBeanFactory
			.instance().makeBean("subscriptionStore");
	private GlobalTopticFilterSubStore globalTopticFilterSubStore = (GlobalTopticFilterSubStore) EvarBeanFactory
			.instance().makeBean("globalTopticFilterSubStore");
	private MqttTaskExecutor mqttTaskExecutor = (MqttTaskExecutor) EvarBeanFactory
			.instance().makeBean("mqttTaskExecutor");
	protected MqttServer mqttServer;

	protected QosPublisher(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}

	public abstract boolean publish(Client client, MqttPublishMessage msg);

	protected QosPublisher(AuthService authService) {
		this.authService = authService;
	}

	protected boolean canPublish(Client client, Topic topic, MqttQoS qos) {
		return authService.canPub(client.getSession().getUserName(), client
				.getSession().getIdentifier(), topic, qos.value());
	}

	public void republish2Subscriber(Client client, int messageId,
			StoredMessage storedMessage) {
		MqttPublishMessage pubMessage = makePublishMessage(messageId, true,
				storedMessage);
		mormitOutboundFlightMessage(client, messageId, storedMessage);
		client.send(MqttMessageType.PUBLISH.name(), pubMessage);
		IntfLoggerUtil.logOutboundMessage(logger,
				MqttMessageType.PUBLISH.name() + "(dup)", client,
				storedMessage.toString());
	}

	public void publish2Subscriber(String identifier,
			StoredMessage storedMessage) {
		Namespace namespace = mqttServer
				.getNamespace(mqttServer.NAMESPACE_MQTT);
		Iterable<Client> clients = namespace.getRoomClients(identifier);
		for (Client client : clients) {
			final int pubMessageId = client.createMessageId();
			publish2Subscriber(client, pubMessageId, storedMessage);
		}
	}

	public void publish2Subscriber(Client client, int messageId,
			StoredMessage storedMessage) {
		mqttTaskExecutor.submit(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				MqttPublishMessage pubMessage = makePublishMessage(messageId,
						false, storedMessage);
				if (pubMessage.fixedHeader().qosLevel() == MqttQoS.AT_LEAST_ONCE
						|| pubMessage.fixedHeader().qosLevel() == MqttQoS.EXACTLY_ONCE) {
					client.addOutboundFlightMessage(messageId, storedMessage);
					mormitOutboundFlightMessage(client, messageId,
							storedMessage);
				}
				client.send(MqttMessageType.PUBLISH.name(), pubMessage);
				IntfLoggerUtil.logOutboundMessage(logger,
						MqttMessageType.PUBLISH.name(), client,
						storedMessage.toString());
			}
		});
	}

	public void publish2Subscribers(StoredMessage storedMessage) {
		// 本节点通知
		List<Subscription> subs = subscriptionStore.match(storedMessage
				.getTopic());
		for (Subscription sub : subs) {
			Namespace namespace = mqttServer
					.getNamespace(mqttServer.NAMESPACE_MQTT);
			Iterable<Client> clients = namespace.getRoomClients(sub
					.getIdentifier());
			for (Client client : clients) {
				final int pubMessageId = client.createMessageId();
				StoredMessage storedMsg = new StoredMessage(storedMessage);
				storedMsg.setQos(Math.min(storedMsg.getQos(), sub.getQos()));
				publish2Subscriber(client, pubMessageId, storedMsg);
			}
		}
		if (!storedMessage.getSource().equals(mqttServer.getServerId())) {
			// 消息非本节点发布，不要通知其他节点
			return;
		}
		// 通知其他节点
		final StoredMessage storedMsg = new StoredMessage(storedMessage);
		if (mqttServer.getClusterType() == ClusterType.PUB_CLUSTER) {
			// 通知所有节点
			mqttServer.getPubSubStore().publish(PubSubType.MQTT_PUB,
					new MqttPubMessage(storedMsg));
		} else if (mqttServer.getClusterType() == ClusterType.SUB_CLUSTER) {
			// 通知其他节点
			Set<String> serverIds = globalTopticFilterSubStore
					.match(storedMessage.getTopic());
			for (String serverId : serverIds) {
				if (!serverId.equals(mqttServer.getServerId())) {
					mqttTaskExecutor.submit(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							MqttNotifierHelper.publish(serverId, storedMsg);
							logger.info("Notify Node={} publish: {}", serverId,
									storedMsg.toString());
						}
					});
				} else {
					// 自身节点，不要重复通知
				}
			}
		}
		else {
			// 单节点
		}
	}

	private void mormitOutboundFlightMessage(Client client, int messageId,
			StoredMessage storedMessage) {
		if (storedMessage.getQos() == MqttQoS.EXACTLY_ONCE.value()) {
			mqttServer.getScheduler().schedule(
					new SchedulerKey(SchedulerKey.Type.QOS2_WAIT_PUBREC,
							client.getSessionId() + ":" + messageId),
					new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							StoredMessage msg = client
									.getOutboundFlightMessage(messageId);
							if (msg != null) {
								// resend publish
								republish2Subscriber(client, messageId,
										storedMessage);
							}
						}
					}, MqttConsts.WaitTimeOut.QOS2_PUBREC_TIMEOUT.value(),
					TimeUnit.SECONDS);
		} else if (storedMessage.getQos() == MqttQoS.AT_LEAST_ONCE.value()) {
			mqttServer.getScheduler().schedule(
					new SchedulerKey(SchedulerKey.Type.QOS1_WAIT_PUBACK,
							client.getSessionId() + ":" + messageId),
					new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							StoredMessage msg = client
									.getOutboundFlightMessage(messageId);
							if (msg != null) {
								// resend publish
								republish2Subscriber(client, messageId,
										storedMessage);
							}
						}
					}, MqttConsts.WaitTimeOut.QOS1_PUBACK_TIMEOUT.value(),
					TimeUnit.SECONDS);
		} else {
			logger.error("Publish not need mornit：qos={}，message={}",
					storedMessage.getQos(), storedMessage.toString());
		}
	}

	private MqttPublishMessage makePublishMessage(int messageId, boolean isDup,
			StoredMessage storedMessage) {
		// TODO wtw ByteBuf使用缓存
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PUBLISH, isDup, MqttQoS.valueOf(storedMessage
						.getQos()), storedMessage.isRetain(), 0);
		MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(
				storedMessage.getTopic().getTopicFilter(), messageId);
		return new MqttPublishMessage(fixedHeader, varHeader,
				Unpooled.wrappedBuffer(storedMessage.getPayload()));
	}

	protected byte[] readBytesAndRewind(ByteBuf payload) {
		byte[] payloadContent = new byte[payload.readableBytes()];
		int mark = payload.readerIndex();
		payload.readBytes(payloadContent);
		payload.readerIndex(mark);
		return payloadContent;
	}

	public AuthService getAuthService() {
		return authService;
	}

	public void setAuthService(AuthService authService) {
		this.authService = authService;
	}

}
