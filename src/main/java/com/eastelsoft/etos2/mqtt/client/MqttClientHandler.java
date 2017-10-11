package com.eastelsoft.etos2.mqtt.client;

import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import io.atomix.catalyst.serializer.collection.ArrayListSerializer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;

import com.eastelsoft.etos2.mqtt.monitor.MonitorFactory;
import com.eastelsoft.etos2.mqtt.util.CancelableScheduler;
import com.eastelsoft.etos2.mqtt.util.HashedWheelScheduler;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

@ChannelHandler.Sharable
public class MqttClientHandler extends SimpleChannelInboundHandler<Object> {
	private static Logger logger = LoggerFactory
			.getLogger(MqttClientHandler.class);
	CancelableScheduler scheduler = new HashedWheelScheduler();
	private AtomicInteger topicId = new AtomicInteger();
	private String topic;
	private int qos = 1;
	private int payloadSize = 128;
	private int pubInterval = 1000;
	private final byte[] payload;

	public MqttClientHandler(String topic) {
		this.topic = topic;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < payloadSize; i++) {
			sb.append(i % 10);
		}
		payload = sb.toString().getBytes();
	}

	public MqttClientHandler(String topic, int qos, int payloadSie,
			int pubInterval) {
		this.topic = topic;
		this.qos = qos;
		this.payloadSize = payloadSize;
		this.pubInterval = pubInterval;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < payloadSize; i++) {
			sb.append(i % 10);
		}
		payload = sb.toString().getBytes();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext arg0, Object arg1)
			throws Exception {
		// TODO Auto-generated method stub
		MqttMessage msg = (MqttMessage) arg1;
		MqttMessageType messageType = msg.fixedHeader().messageType();
		if (messageType == MqttMessageType.CONNACK) {
			MqttClient.connectingCount.decrementAndGet();
			MqttConnAckMessage connAck = (MqttConnAckMessage) msg;
			if (connAck.variableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
				MqttClient.connectedCount.incrementAndGet();
				arg0.channel().attr(MqttClient.MQTT_CONNECTED).set(true);
				MonitorFactory.getInstance().metricsCount(
						MqttClient.MQTT_CLIENT, MqttMessageType.CONNACK.name(),
						true);
				MonitorFactory
						.getInstance()
						.metricsDelay(
								MqttClient.MQTT_CLIENT,
								MqttMessageType.CONNACK.name(),
								(System.currentTimeMillis())
										- arg0.channel()
												.attr(MqttClient.MQTT_CONNECT_BEGIN_TIME)
												.get());
				if (!StringUtils.isEmpty(this.topic)) {
					arg0.channel().attr(MqttClient.MQTT_MSGID)
							.set(new AtomicInteger());
					String myTopic = topic + "/" + topicId.incrementAndGet();
					subscribe(arg0, myTopic, qos);
					if (pubInterval > 0) {
						publish(arg0, myTopic, qos, payloadSize, pubInterval,
								this.payload, createMessageId(arg0));
					}
				}
			} else {
				MonitorFactory.getInstance().metricsCount(
						MqttClient.MQTT_CLIENT, MqttMessageType.CONNACK.name(),
						false);
			}
		} else if (messageType == MqttMessageType.PUBACK) {
			MonitorFactory.getInstance().metricsCount(MqttClient.MQTT_CLIENT,
					MqttMessageType.PUBACK.name(), true);
			MonitorFactory.getInstance().metricsDelay(
					MqttClient.MQTT_CLIENT,
					MqttMessageType.PUBACK.name(),
					(System.currentTimeMillis())
							- arg0.channel()
									.attr(MqttClient.MQTT_PUBLISH_BEGIN_TIME)
									.get());
		} else if (messageType == MqttMessageType.SUBACK) {
			MonitorFactory.getInstance().metricsCount(MqttClient.MQTT_CLIENT,
					MqttMessageType.SUBACK.name(), true);
			MonitorFactory.getInstance().metricsDelay(
					MqttClient.MQTT_CLIENT,
					MqttMessageType.SUBACK.name(),
					(System.currentTimeMillis())
							- arg0.channel()
									.attr(MqttClient.MQTT_SUBSCRIBE_BEGIN_TIME)
									.get());
		} else if (messageType == MqttMessageType.PUBLISH) {
			MqttPublishMessage pubMsg = (MqttPublishMessage) msg;
			if (pubMsg.fixedHeader().qosLevel() == MqttQoS.AT_MOST_ONCE) {
				;
			} else if (pubMsg.fixedHeader().qosLevel() == MqttQoS.AT_LEAST_ONCE) {
				sendPubAck(arg0, pubMsg.variableHeader().messageId());
			} else if (pubMsg.fixedHeader().qosLevel() == MqttQoS.EXACTLY_ONCE) {
				sendPubReceived(arg0, pubMsg.variableHeader().messageId());
			}
		} else if (messageType == MqttMessageType.PUBREL) {
			int messageId = ((MqttMessageIdVariableHeader) msg.variableHeader())
					.messageId();
			sendPubComp(arg0, messageId);
		} else if (messageType == MqttMessageType.PINGRESP) {
			;
		} else {
			logger.error("MqttMessageType {} not deal", messageType);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		// 超时处理
		if (evt instanceof IdleStateEvent) {
			MqttFixedHeader pingHeader = new MqttFixedHeader(
					MqttMessageType.PINGREQ, false, AT_MOST_ONCE, false, 0);
			MqttMessage ping = new MqttMessage(pingHeader);
			ctx.writeAndFlush(ping);
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		logger.error("Client {} catched exception {}", ctx.channel()
				.remoteAddress(), cause);
		ctx.close();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		// 会话断开处理
		if (ctx.channel().attr(MqttClient.MQTT_CONNECTED).get() != null) {
			MqttClient.connectedCount.decrementAndGet();
		}
	}

	private String getIp(SocketAddress socketAddress) {
		if (socketAddress != null) {
			String ipAndPort = socketAddress.toString();
			if (ipAndPort.contains("/")) {
				ipAndPort = ipAndPort.substring(ipAndPort.indexOf("/") + 1);
			}
			if (ipAndPort.contains(":")) {
				return ipAndPort.substring(0, ipAndPort.indexOf(":"));
			}
			return ipAndPort;
		}
		return "";
	}

	private int getPort(SocketAddress socketAddress) {
		if (socketAddress != null) {
			String ipPort = socketAddress.toString();
			if (ipPort.contains("/")) {
				ipPort = ipPort.substring(ipPort.indexOf("/") + 1);
			}
			if (ipPort.contains(":")) {
				ipPort = ipPort.substring(ipPort.indexOf(":") + 1);
				try {
					return Integer.parseInt(ipPort);
				} catch (Exception e) {
					logger.error(e);
					return 0;
				}
			}
		}
		return 0;
	}

	private void subscribe(ChannelHandlerContext ctx, String topic, int qos) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.SUBSCRIBE, false, MqttQoS.valueOf(qos), false,
				0);
		MqttMessageIdVariableHeader varHeader = MqttMessageIdVariableHeader
				.from(createMessageId(ctx));
		MqttTopicSubscription[] subs = new MqttTopicSubscription[] { new MqttTopicSubscription(
				topic, MqttQoS.valueOf(qos)) };
		MqttSubscribePayload payload = new MqttSubscribePayload(
				Arrays.asList(subs));
		MqttSubscribeMessage subMsg = new MqttSubscribeMessage(fixedHeader,
				varHeader, payload);
		ctx.channel().attr(MqttClient.MQTT_SUBSCRIBE_BEGIN_TIME)
				.set(System.currentTimeMillis());
		ctx.writeAndFlush(subMsg);

	}

	private void publish(ChannelHandlerContext ctx, String topic, int qos,
			int payloadSize, int pubInterval, byte[] payload, int messageId) {
		// TODO Auto-generated method stub
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PUBLISH, false, MqttQoS.valueOf(qos), false, 0);
		MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(
				topic, messageId);
		MqttPublishMessage pubMsg = new MqttPublishMessage(fixedHeader,
				varHeader, Unpooled.wrappedBuffer(payload));
		ctx.channel().attr(MqttClient.MQTT_PUBLISH_BEGIN_TIME)
				.set(System.currentTimeMillis());
		ctx.writeAndFlush(pubMsg);
		if (pubInterval > 0) {
			scheduler.schedule(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					if (ctx.channel().isActive()) {
						publish(ctx, topic, qos, payloadSize, pubInterval,
								payload, createMessageId(ctx));
					}
				}
			}, pubInterval, TimeUnit.MILLISECONDS);
		}
	}

	private void sendPubAck(ChannelHandlerContext ctx, int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader,
				from(messageId));
		ctx.writeAndFlush(pubAckMessage);
	}

	private void sendPubReceived(ChannelHandlerContext ctx, int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MqttMessage pubReceived = new MqttMessage(fixedHeader, from(messageId));
		ctx.writeAndFlush(pubReceived);
	}

	private void sendPubComp(ChannelHandlerContext ctx, int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PUBCOMP, false, AT_MOST_ONCE, false, 0);
		MqttMessage pubCompMessage = new MqttMessage(fixedHeader,
				from(messageId));
		ctx.writeAndFlush(pubCompMessage);
	}

	private int createMessageId(ChannelHandlerContext ctx) {
		return ctx.channel().attr(MqttClient.MQTT_MSGID).get()
				.incrementAndGet() % 0xFFFF;

	}

}
