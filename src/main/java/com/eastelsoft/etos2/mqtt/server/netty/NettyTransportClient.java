package com.eastelsoft.etos2.mqtt.server.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;

import java.net.SocketAddress;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.Client.Transport;
import com.eastelsoft.etos2.mqtt.server.TransportClient;

import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.IdGenerator;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class NettyTransportClient implements TransportClient {
	private final Logger log = LoggerFactory
			.getLogger(NettyTransportClient.class);
	public static final AttributeKey<Client> CLIENT = AttributeKey
			.<Client> valueOf("client");// 请求client对象
	public static final AttributeKey<Object> REQ_MSG = AttributeKey
			.<Object> valueOf("req_msg");// 请求消息
	public static final AttributeKey<Integer> PINT_TIMEOUT_COUNT = AttributeKey
			.<Integer> valueOf("PINT_TIMEOUT_COUNT");// Ping Timeout 次数
	public static AttributeKey<String> JSONP_TAG = AttributeKey
			.<String> valueOf("JSONP_TAG");// JSON调用
	private final Channel channel;
	private final String sessionId;
	private final Transport transport;

	public NettyTransportClient(Channel channel) {
		this(IdGenerator.createObjectIdHex(), channel);
	}

	public NettyTransportClient(String sessionId, Channel channel) {
		this(sessionId, channel, Transport.TCP);
	}

	public NettyTransportClient(String sessionId, Channel channel,
			Transport transport) {
		this.sessionId = sessionId;
		this.channel = channel;
		this.transport = transport;
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		if (channel != null) {
			channel.close();
		}
	}
	
	@Override
	public boolean isActive() {
		if (channel != null) {
			return channel.isActive();
		}
		return false;
	}

	@Override
	public String getSessionId() {
		// TODO Auto-generated method stub
		return sessionId;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		if (channel != null) {
			return channel.remoteAddress();
		} else {
			return null;
		}
	}

	@Override
	public Transport getTransport() {
		// TODO Auto-generated method stub
		return this.transport;
	}

	@Override
	public String getRemoteIp() {
		return channel != null ? getIp(channel.remoteAddress()) : null;
	}

	@Override
	public void send(Object data) {
		// TODO Auto-generated method stub
		channel.writeAndFlush(data);
	}

	@Override
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		if (channel != null) {
			ChannelPipeline pipeline = channel.pipeline();
			if (pipeline.names().contains(
					NettyMqttServer.MQTT_IDLESTATE_HANDLER)) {
				pipeline.remove(NettyMqttServer.MQTT_IDLESTATE_HANDLER);
			}
			pipeline.addFirst(NettyMqttServer.MQTT_IDLESTATE_HANDLER,
					new IdleStateHandler(keepAliveSeconds, 0, 0));
		}
	}

	private String getIp(SocketAddress socketAddress) {
		if (socketAddress != null) {
			String ipPort = socketAddress.toString();
			if (ipPort.contains("/")) {
				ipPort = ipPort.substring(ipPort.indexOf("/") + 1);
			}
			if (ipPort.contains(":")) {
				return ipPort.substring(0, ipPort.indexOf(":"));
			}
			return ipPort;
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
					log.error(e.getMessage(), e);
					return 0;
				}
			}
		}
		return 0;
	}
}
