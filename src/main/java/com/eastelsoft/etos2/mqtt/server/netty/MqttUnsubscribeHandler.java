package com.eastelsoft.etos2.mqtt.server.netty;

import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;

import java.util.List;
import java.util.Map;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.Subscription;
import com.eastelsoft.etos2.mqtt.server.SubscriptionStore;
import com.eastelsoft.etos2.mqtt.server.Topic;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;

import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttUnsubscribeHandler extends
		MqttMessageHandler<MqttUnsubscribeMessage> {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttUnsubscribeHandler.class);
	private final SubscriptionStore subscriptionStore = (SubscriptionStore) EvarBeanFactory
			.instance().makeBean("subscriptionStore");
	private final MqttServer mqttServer;

	public MqttUnsubscribeHandler(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}

	@Override
	public void onData(Client client, Map<String, List<String>> parameters,
			MqttUnsubscribeMessage data) throws Exception {
		// TODO Auto-generated method stub
		IntfLoggerUtil.logInboundMessage(logger,
				MqttMessageType.UNSUBSCRIBE.name(), client, data.toString());
		final int messageId = MqttProtoHelper.messageId(data);
		List<String> topics = data.payload().topics();
		for (String t : topics) {
			Topic topic = new Topic(t);
			if (!topic.isValid()) {
				continue;
			}
			Subscription subscription = new Subscription(client.getSessionId(),
					client.getSession().getIdentifier(), topic, 0,
					mqttServer.getServerId());
			client.unsub(subscription);
			subscriptionStore.unsub(subscription);
		}
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttUnsubAckMessage unSubAck = new MqttUnsubAckMessage(fixedHeader, from(messageId));
        client.send(MqttMessageType.UNSUBACK.name(), unSubAck);
        IntfLoggerUtil.logOutboundMessage(logger,
				MqttMessageType.UNSUBACK.name(), client, unSubAck.toString());
	}
}
