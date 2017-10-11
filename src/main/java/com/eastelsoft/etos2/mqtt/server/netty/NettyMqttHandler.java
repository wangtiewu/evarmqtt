package com.eastelsoft.etos2.mqtt.server.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.net.SocketAddress;

import org.apache.commons.lang.StringUtils;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.Namespace;
import com.eastelsoft.etos2.mqtt.server.NamespaceClient;
import com.eastelsoft.etos2.mqtt.server.TransportClient;
import com.eastelsoft.etos2.mqtt.util.CancelableScheduler;

import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.IdGenerator;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;
import eet.evar.tool.trace.TraceContext;
import eet.evar.tool.trace.TraceContextUtil;

@ChannelHandler.Sharable
public class NettyMqttHandler extends SimpleChannelInboundHandler<Object> {
	private static Logger logger = LoggerFactory
			.getLogger(NettyMqttHandler.class);
	private static final int HTTP_CONTENT_MAX_SIZE = 1024 * 1024;// 1M，消息体最大值1M
	private final String serverId;// 服务器ID
	private final Namespace namespace;
	private final long serverStartTime;// 服务启动时间
	private final CancelableScheduler scheduler;// 调度器
	private final MqttServer mqttServer;
	private static final Redis redis = (Redis) EvarBeanFactory.instance()
			.makeBean("redis");

	public NettyMqttHandler(Namespace namespace, MqttServer mqttServer) {
		this.serverId = mqttServer.getServerId();
		this.serverStartTime = mqttServer.getServerStartTime();
		this.namespace = namespace;
		this.scheduler = mqttServer.getScheduler();
		this.mqttServer = mqttServer;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext arg0, Object arg1)
			throws Exception {
		// TODO Auto-generated method stub
		MqttMessage msg = (MqttMessage) arg1;
		MqttMessageType messageType = msg.fixedHeader().messageType();
		logger.info("Processing MQTT message, type={}，client={}", messageType,
				arg0.channel().remoteAddress());
//		if (true) {
//			MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
//					MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
//					false, 0);
//			MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(
//					MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
//			MqttConnAckMessage connAck = new MqttConnAckMessage(
//					mqttFixedHeader, mqttConnAckVariableHeader);
//			arg0.writeAndFlush(connAck);
//			return;
//		}
		Client client = arg0.channel().attr(NettyTransportClient.CLIENT).get();
		if (client != null) {
			if (messageType != MqttMessageType.CONNECT) {
				if (client.getSession() == null) {
					// 未认证
					logger.error("{} not authed：remoteAddress={}, close it",
							client.getSessionId(), arg0.channel()
									.remoteAddress());
					arg0.channel().close();
					return;
				}
			}
			namespace.onEvent(client, messageType.name(), null, msg);
		} else {
			// 未绑定Client对象
			logger.error("Client {} not binded , close it", arg0.channel()
					.remoteAddress());
			arg0.channel().close();
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		// 超时处理
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			logger.debug("Client {} IdleStateEvent", ctx.channel()
					.remoteAddress());
			Client client = ctx.channel().attr(NettyTransportClient.CLIENT)
					.get();
			if (client != null) {
				namespace.onPingTimeout(client, 0);
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		logger.error("Client {} catched exception {}", ctx.channel()
				.remoteAddress(), cause);
		Client client = ctx.channel().attr(NettyTransportClient.CLIENT).get();
		if (client != null) {
			namespace.onException(client, cause);
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		logger.info("Client {} connected", ctx.channel().remoteAddress());
		TransportClient transportClient = new NettyTransportClient(
				ctx.channel());
		Client client = new NamespaceClient(transportClient, namespace,
				mqttServer);
		ctx.channel().attr(NettyTransportClient.CLIENT).set(client);
		namespace.onConnect(client);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		// 会话断开处理
		logger.info("Client {} disconnected", ctx.channel().remoteAddress());
		Client client = ctx.channel().attr(NettyTransportClient.CLIENT).get();
		if (client != null) {
			namespace.onDisconnect(client);
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

	private void saveTraceContext(Channel channel, FullHttpRequest httpReq,
			String api) {
		// TODO Auto-generated method stub
		String txId = httpReq.headers().get(TraceContext.HTTP_HEAD_TXID);
		String pSpanId = httpReq.headers().get(TraceContext.HTTP_HEAD_SPANID);
		if (StringUtils.isEmpty(txId)) {
			txId = IdGenerator.createObjectIdHex();
		}
		if (StringUtils.isEmpty(pSpanId)) {
			pSpanId = TraceContext.ROOT_SPAN_ID;
		}
		TraceContext traceContext = new TraceContext(txId, pSpanId, serverId);
		traceContext.setApi(api);
		channel.attr(TraceContext.TRACE_CONTEXT).set(traceContext);
		TraceContextUtil.setTraceContext(traceContext);
	}

}
