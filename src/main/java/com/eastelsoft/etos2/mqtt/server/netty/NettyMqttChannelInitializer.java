package com.eastelsoft.etos2.mqtt.server.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import com.eastelsoft.etos2.mqtt.server.Configuration;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.Namespace;
import com.eastelsoft.etos2.mqtt.server.NamespacesHub;
import com.eastelsoft.etos2.mqtt.util.CancelableScheduler;

public class NettyMqttChannelInitializer extends
		ChannelInitializer<SocketChannel> {
	private ChannelHandler nettyMqttHandler = null;
	private final MqttServer mqttServer;

	public NettyMqttChannelInitializer(Namespace namespace,
			MqttServer mqttServer) {
		this.mqttServer = mqttServer;
		nettyMqttHandler = new NettyMqttHandler(namespace, mqttServer);
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(NettyMqttServer.MQTT_DECODER, new MqttDecoder());
		ch.pipeline().addLast(NettyMqttServer.MQTT_ENCODER,
				MqttEncoder.INSTANCE);
		ch.pipeline().addLast(
				NettyMqttServer.MQTT_IDLESTATE_HANDLER,
				new IdleStateHandler(mqttServer.getConfiguration()
						.getPingInterval(), 0, 0));
		ch.pipeline().addLast(NettyMqttServer.MQTT_HANDLER, nettyMqttHandler);
	}
}
