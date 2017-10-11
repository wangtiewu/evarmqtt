package com.eastelsoft.etos2.mqtt.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import com.eastelsoft.etos2.mqtt.monitor.MonitorFactory;

import eet.evar.StringDeal;
import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.IdGenerator;
import eet.evar.tool.NetUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;

public class MqttClient {
	public static final AttributeKey<Boolean> MQTT_CONNECTED = AttributeKey
			.valueOf("MQTT_CONNECTED");
	public static final AttributeKey<Long> MQTT_CONNECT_BEGIN_TIME = AttributeKey
			.valueOf("MQTT_CONNECT_BEGIN_TIME");
	public static final AttributeKey<Long> MQTT_PUBLISH_BEGIN_TIME = AttributeKey
			.valueOf("MQTT_PUBLISH_BEGIN_TIME");
	public static final AttributeKey<Long> MQTT_SUBSCRIBE_BEGIN_TIME = AttributeKey
			.valueOf("MQTT_SUBSCRIBE_BEGIN_TIME");
	public static final AttributeKey<AtomicInteger> MQTT_MSGID = AttributeKey
			.valueOf("MQTT_MSGID");
	public static final String MQTT_CLIENT = "mqtt-client";
	public static AtomicInteger connectedCount = new AtomicInteger();
	public static AtomicInteger connectingCount = new AtomicInteger();
	private static EventLoopGroup group = new NioEventLoopGroup(Runtime
			.getRuntime().availableProcessors() * 2);
	private static Bootstrap clientBootstrap = new Bootstrap();
	MqttClientHandler mqttClientHandler;
	static {
		clientBootstrap.group(group).channel(NioSocketChannel.class)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new ChannelInitializer() {
					@Override
					protected void initChannel(Channel ch) throws Exception {
						// TODO Auto-generated method stub
						ch.pipeline().addLast(new MqttDecoder());
						ch.pipeline().addLast(MqttEncoder.INSTANCE);
					}
				}).option(ChannelOption.SO_SNDBUF, 1024)
				.option(ChannelOption.SO_RCVBUF, 1024);
	}

	public void connect(String brokerHost, int port, String idPrefix,
			String userName, String password, boolean clean, int pingInterval,
			String topic, int qos, int payloadSie, int pubInterval, int count,
			int connInterval, String[] localHosts, boolean onlyConn) {
		mqttClientHandler = new MqttClientHandler(topic, qos, payloadSie,
				pubInterval);
		if (localHosts == null || localHosts.length < 1) {
			localHosts = new String[] { NetUtils.getLocalHost() };
		}
		final SocketAddress remoteAddress = new InetSocketAddress(brokerHost,
				port);
		for (String localHost : localHosts) {
			for (int p = 1025; p <= 65530; p++) {
				if (connectedCount.get() >= count) {
					return;
				}
				if (connectingCount.get() > 800) {
					try {
						Thread.sleep(connInterval);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				SocketAddress localAddress1 = new InetSocketAddress(localHost,
						p);
				ChannelFuture future = connect(remoteAddress, localAddress1);
				future.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future)
							throws Exception {
						if (future.isSuccess()) {
							// send connect
							future.channel()
									.pipeline()
									.addLast(
											new IdleStateHandler(pingInterval,
													0, 0));
							future.channel().pipeline()
									.addLast(mqttClientHandler);
							if (!onlyConn) {
								sendConnectPackage(future.channel(), idPrefix,
										userName, password, clean, pingInterval);
							}
						} else {
							connectingCount.decrementAndGet();
							System.out.println("connect fail: "
									+ future.cause());
						}
					}
				});
			}
		}
	}

	protected void sendConnectPackage(Channel channel, String idPrefix,
			String userName, String password, boolean clean, int pingInterval) {
		// TODO Auto-generated method stub
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MqttConnectVariableHeader varHeader = new MqttConnectVariableHeader(
				"MQTT", 4, true, true, false, 0, false, clean, pingInterval);
		MqttConnectPayload payload = new MqttConnectPayload(idPrefix + "_"
				+ IdGenerator.createObjectIdHex(), "", "", userName, password);
		MqttConnectMessage connMessage = new MqttConnectMessage(fixedHeader,
				varHeader, payload);
		channel.writeAndFlush(connMessage);
		MonitorFactory.getInstance().metricsCount(MQTT_CLIENT,
				MqttMessageType.CONNECT.name(), true);
		channel.attr(MQTT_CONNECT_BEGIN_TIME).set(System.currentTimeMillis());
	}

	private ChannelFuture connect(SocketAddress remoteAddress,
			SocketAddress localAddress) {
		ChannelFuture connectFuture = clientBootstrap.connect(remoteAddress,
				localAddress);
		connectingCount.incrementAndGet();
		return connectFuture;
	}

	public static void main(String[] args) {
		EvarBeanFactory.instance(new String[] {
				"spring-bean-container-cache.xml",
				"spring-bean-container-datasource.xml" });
		String brokerHost = "10.0.69.28";
		int port = 1883;
		int count = 1;
		int connInterval = 0;
		String idPrefix = "test";
		String userName = "";
		String password = "";
		boolean clean = true;
		int pingInterval = 10;
		String topicPrefix = "test";
		int payloadSize = 64;
		int pubInterval = 0;
		int qos = 1;
		String[] localAddresses = null;
		boolean onlyConn = false;
		for (String arg : args) {
			if (arg.startsWith("-h")) {
				brokerHost = StringDeal.split(arg, "=")[1];
			} else if (arg.startsWith("-p")) {
				port = Integer.valueOf(StringDeal.split(arg, "=")[1]);
			} else if (arg.startsWith("-c")) {
				count = Integer.valueOf(StringDeal.split(arg, "=")[1]);
			} else if (arg.startsWith("-u")) {
				userName = StringDeal.split(arg, "=")[1];
			} else if (arg.startsWith("-P")) {
				password = StringDeal.split(arg, "=")[1];
			} else if (arg.startsWith("-id")) {
				idPrefix = StringDeal.split(arg, "=")[1];
			} else if (arg.startsWith("-C")) {
				clean = Boolean.valueOf(StringDeal.split(arg, "=")[1]);
			} else if (arg.startsWith("-k")) {
				pingInterval = Integer.valueOf(StringDeal.split(arg, "=")[1]);
			} else if (arg.startsWith("-ifaddr")) {
				localAddresses = StringDeal.split(
						StringDeal.split(arg, "=")[1], ",");
			} else if (arg.startsWith("-t")) {
				topicPrefix = StringDeal.split(arg, "=")[1];
			} else if (arg.startsWith("-s")) {
				payloadSize = Integer.valueOf(StringDeal.split(arg, "=")[1]);
			} else if (arg.startsWith("-I")) {
				pubInterval = Integer.valueOf(StringDeal.split(arg, "=")[1]);
			} else if (arg.startsWith("-q")) {
				qos = Integer.valueOf(StringDeal.split(arg, "=")[1]);
			} else if (arg.startsWith("-i")) {
				connInterval = Integer.valueOf(StringDeal.split(arg, "=")[1]);
			} else if (arg.startsWith("-O")) {
				onlyConn = Boolean.valueOf(StringDeal.split(arg, "=")[1]);
			}
		}
		if (brokerHost.equals("localhost") || brokerHost.equals("127.0.0.1")) {
			localAddresses = new String[] { "localhost" };
		} else if (localAddresses == null || localAddresses.length < 1) {
			localAddresses = new String[] { NetUtils.getLocalHost() };
		}
		new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("connected=" + connectedCount.get());
				}
			}
		}.start();
		MqttClient mqttClient = new MqttClient();
		mqttClient.connect(brokerHost, port, idPrefix, userName, password,
				clean, pingInterval, topicPrefix, qos, payloadSize,
				pubInterval, count, connInterval, localAddresses, onlyConn);
		while (true) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
