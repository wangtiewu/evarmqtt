package com.eastelsoft.etos2.mqtt.cluster;

import com.eastelsoft.etos2.mqtt.cluster.message.ConnectMessage;
import com.eastelsoft.etos2.mqtt.cluster.message.DisconnectMessage;
import com.eastelsoft.etos2.mqtt.cluster.message.DispatchMessage;
import com.eastelsoft.etos2.mqtt.cluster.message.JoinLeaveMessage;
import com.eastelsoft.etos2.mqtt.cluster.message.MqttPubMessage;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.Namespace;
import com.eastelsoft.etos2.mqtt.server.NamespacesHub;
import com.eastelsoft.etos2.mqtt.server.SubscriptionStore;
import com.eastelsoft.etos2.mqtt.server.netty.QosPublisher;
import com.eastelsoft.etos2.mqtt.server.netty.QosPublisherFactory;
import com.eastelsoft.etos2.mqtt.util.JsonSupport;

import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public abstract class BaseStoreFactory implements StoreFactory {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private Long nodeId = (long) (Math.random() * 1000000);
	private MqttServer mqttServer;

	protected Long getNodeId() {
		return nodeId;
	}

	@Override
	public void init(final MqttServer mqttServer,
			final NamespacesHub namespacesHub, JsonSupport jsonSupport) {
		this.mqttServer = mqttServer;
		pubSubStore().subscribe(PubSubType.DISCONNECT,
				new PubSubListener<DisconnectMessage>() {
					@Override
					public void onMessage(DisconnectMessage msg) {
						log.debug("{} sessionId: {}", PubSubType.DISCONNECT,
								msg.getSessionId());
					}
				}, DisconnectMessage.class);

		pubSubStore().subscribe(PubSubType.CONNECT,
				new PubSubListener<ConnectMessage>() {
					@Override
					public void onMessage(ConnectMessage msg) {
						log.debug("{} sessionId: {}", PubSubType.CONNECT,
								msg.getSessionId());
					}
				}, ConnectMessage.class);

		pubSubStore().subscribe(PubSubType.DISPATCH,
				new PubSubListener<DispatchMessage>() {
					@Override
					public void onMessage(DispatchMessage msg) {
						String name = msg.getRoom();
						Namespace namespace = namespacesHub.get(msg
								.getNamespace());
						if (namespace != null) {
							namespace.dispatch(name, msg.getPayload());
						}
						log.debug("{} packet: {}", PubSubType.DISPATCH,
								msg.getPayload());
					}
				}, DispatchMessage.class);

		pubSubStore().subscribe(PubSubType.JOIN,
				new PubSubListener<JoinLeaveMessage>() {
					@Override
					public void onMessage(JoinLeaveMessage msg) {
						String name = msg.getRoom();
						Namespace namespace = namespacesHub.get(msg
								.getNamespace());
						if (namespace != null) {
							namespace.join(name, msg.getSessionId().toString());
						}
						log.debug("{} sessionId: {}", PubSubType.JOIN,
								msg.getSessionId());
					}
				}, JoinLeaveMessage.class);

		pubSubStore().subscribe(PubSubType.LEAVE,
				new PubSubListener<JoinLeaveMessage>() {
					@Override
					public void onMessage(JoinLeaveMessage msg) {
						String name = msg.getRoom();
						Namespace namespace = namespacesHub.get(msg
								.getNamespace());
						if (namespace != null) {
							namespace
									.leave(name, msg.getSessionId().toString());
						}
						log.debug("{} sessionId: {}", PubSubType.LEAVE,
								msg.getSessionId());
					}
				}, JoinLeaveMessage.class);

		pubSubStore().subscribe(PubSubType.MQTT_PUB,
				new PubSubListener<MqttPubMessage>() {
					@Override
					public void onMessage(MqttPubMessage msg) {
						log.info("收到节点 {} 发布的mqtt_pub消息 : {}", msg
								.getStoredMessage().getSource(), msg
								.getStoredMessage());
						QosPublisher qosPublisher = QosPublisherFactory
								.makeQosPublisher(msg.getStoredMessage()
										.getQos(), mqttServer);
						qosPublisher.publish2Subscribers(msg.getStoredMessage());
					}
				}, MqttPubMessage.class);

	}

	@Override
	public abstract PubSubStore pubSubStore();

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ " (distributed session store, distributed publish/subscribe)";
	}

}
