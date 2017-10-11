package com.eastelsoft.etos2.mqtt.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.eastelsoft.etos2.mqtt.MqttConsts.ClusterType;
import com.eastelsoft.etos2.mqtt.cluster.redisson.RedissonStoreFactory;
import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.Client.ConnectionCloseType;
import com.eastelsoft.etos2.mqtt.server.Configuration;
import com.eastelsoft.etos2.mqtt.server.ConnectListener;
import com.eastelsoft.etos2.mqtt.server.DisconnectListener;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.Namespace;
import com.eastelsoft.etos2.mqtt.server.PingTimeoutListener;
import com.eastelsoft.etos2.mqtt.server.RedisKeyBuilder;
import com.eastelsoft.etos2.mqtt.server.Session;
import com.eastelsoft.etos2.mqtt.server.SocketConfig;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.server.SubscriptionStore;
import com.eastelsoft.etos2.mqtt.server.WillMessageStore;
import com.eastelsoft.etos2.mqtt.util.RedisBatchProcUtil;
import com.eastelsoft.etos2.mqtt.util.SchedulerKey;

import eet.evar.StringDeal;
import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.NetUtils;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

/**
 * Fully thread-safe.
 * 
 */
public class NettyMqttServer extends MqttServer {
	private static final Logger logger = LoggerFactory
			.getLogger(NettyMqttServer.class);
	private static final String mqttConfigureFile = "server_mqtt.properties";
	protected static final String TRAFFIC_HANDLER = "trafficHandler";
	protected static final String SSL_HANDLER = "sslHandler";
	protected static final String MQTT_DECODER = "mqttDecoder";
	protected static final String MQTT_ENCODER = "mqttEncoder";
	protected static final String MQTT_IDLESTATE_HANDLER = "idleStateHandler";
	protected static final String MQTT_HANDLER = "mqttHandler";
	protected EventLoopGroup bossGroup;
	protected EventLoopGroup workerGroup;
	protected GlobalTrafficShapingHandler globalTrafficShapingHandler = new GlobalTrafficShapingHandler(
			Executors.newScheduledThreadPool(1), 60000);
	private final WillMessageStore willMessageStore = (WillMessageStore) EvarBeanFactory
			.instance().makeBean("willMessageStore");
	private final SubscriptionStore subscriptionStore = (SubscriptionStore) EvarBeanFactory
			.instance().makeBean("subscriptionStore");

	public NettyMqttServer() {
		this(null);
	}

	public NettyMqttServer(String mqttConfigureFile) {
		this(false, false);
		configuration.getStoreFactory().init(this, namespacesHub,
				configuration.getJsonSupport());
	}

	public NettyMqttServer(boolean addIntfTraceLogListener,
			boolean addIntfPerfListener) {
		super(addIntfTraceLogListener, addIntfPerfListener);
		try {
			if (configuration.getKeyStore() != null) {
				serverSSLContext = createSSLContext(configuration);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Adds the ssl handler
	 *
	 * @return
	 */
	protected void addSslHandler(ChannelPipeline pipeline) {
		if (serverSSLContext != null) {
			SSLEngine engine = serverSSLContext.createSSLEngine();
			engine.setUseClientMode(false);
			if (configuration.getTrustStore() != null) {
				engine.setNeedClientAuth(true);
			}
			pipeline.addLast(SSL_HANDLER, new SslHandler(engine));
		}
	}

	protected SslHandler getSslHandler() {
		if (serverSSLContext != null) {
			SSLEngine engine = serverSSLContext.createSSLEngine();
			engine.setUseClientMode(false);
			if (configuration.getTrustStore() != null) {
				engine.setNeedClientAuth(true);
			}
			return new SslHandler(engine);
		} else {
			logger.error("serverSSLContext is null");
			return null;
		}
	}

	protected void initMyHttpChannel(ChannelPipeline pipeline,
			ChannelHandler myChannelInboundHandler) {
	}

	/**
	 * 最近统计周期的读入字节数
	 * 
	 * @return
	 */
	public long lastReadBytes() {
		return globalTrafficShapingHandler != null ? globalTrafficShapingHandler
				.trafficCounter().lastReadBytes() : 0;
	}

	/**
	 * 最近统计周期的写入字节数
	 * 
	 * @return
	 */
	public long lastWrittenBytes() {
		return globalTrafficShapingHandler != null ? globalTrafficShapingHandler
				.trafficCounter().lastWrittenBytes() : 0;
	}

	/**
	 * 最近统计周期读bytes/s
	 * 
	 * @return
	 */
	public long lastReadThroughput() {
		return globalTrafficShapingHandler != null ? globalTrafficShapingHandler
				.trafficCounter().lastReadThroughput() : 0L;
	}

	/**
	 * 最近统计周期写bytes/s
	 * 
	 * @return
	 */
	public long lastWriteThroughput() {
		return globalTrafficShapingHandler != null ? globalTrafficShapingHandler
				.trafficCounter().lastWriteThroughput() : 0L;
	}

	/**
	 * Start server
	 */
	public boolean start() {
		Namespace namespace = initNamespace();
		initGroups();
		Class channelClass = NioServerSocketChannel.class;
		if (configuration.isUseLinuxNativeEpoll()) {
			channelClass = EpollServerSocketChannel.class;
		}
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup).channel(channelClass)
				.childHandler(new NettyMqttChannelInitializer(namespace, this));
		applyConnectionOptions(b);
		InetSocketAddress addr = new InetSocketAddress(configuration.getPort());
		if (configuration.getHostname() != null) {
			addr = new InetSocketAddress(configuration.getHostname(),
					configuration.getPort());
		}
		try {
			b.bind(addr).sync();
			logger.info("Mqtt Broker服务已启动：serverId={}，host={}，port={}",
					configuration.getServerId(), configuration.getHostname(),
					configuration.getPort());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	/**
	 * Stop server
	 */
	public void stop() {
		if (bossGroup != null) {
			bossGroup.shutdownGracefully().syncUninterruptibly();
		}
		if (workerGroup != null) {
			workerGroup.shutdownGracefully().syncUninterruptibly();
		}
		super.stop();
		logger.info("Mqtt Broker服务{}:{}已停止", configuration.getHostname(),
				configuration.getPort());
	}

	private Namespace initNamespace() {
		// TODO Auto-generated method stub
		final Namespace namespace = addNamespace(NAMESPACE_MQTT);
		namespace.addConnectListener(new ConnectListener() {

			@Override
			public void onConnect(final Client client) {
				// TODO Auto-generated method stub
				logger.info("{} connected: remoteAddress={}, transport={}",
						client.getSessionId(), client.getRemoteAddress(),
						client.getTransport());
				// 15秒登陆检测
				scheduler.schedule(
						new SchedulerKey(SchedulerKey.Type.LOGIN_TIMEOUT,
								client.getSessionId()), new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								if (client.getSession() == null) {
									logger.error(client.getSessionId()
											+ " 15秒内未登陆，关闭连接："
											+ client.getRemoteAddress());
									client.disconnect();
								}
							}
						}, 15, TimeUnit.SECONDS);
			}
		});

		namespace.addDisconnectListener(new DisconnectListener() {

			@Override
			public void onDisconnect(Client client) {
				// TODO Auto-generated method stub
				Session session = client.getSession();
				if (session != null) {
					logger.info(
							"{} disconnected: identifier={}, remoteAddress={}",
							client.getSessionId(), session.getIdentifier(),
							client.getRemoteAddress());
					if (client.getConnCloseType() == ConnectionCloseType.PROTO_DISCONNECTED) {
						willMessageStore.remove(session.getIdentifier());
						subscriptionStore.unsub(client.allSubs());
						if (session.getSessionId()
								.equals(client.getSessionId())) {
							RedisBatchProcUtil.submitDel(RedisKeyBuilder
									.buildSessionKey(session.getIdentifier()));
						}
						client.destroy();
					} else if (client.getConnCloseType() == ConnectionCloseType.DUP_CONNECT) {
						subscriptionStore.unsub(client.allSubs());
						client.destroy();
					} else {
						if (session.isWillMessageFlag()) {
							// publish will message
							StoredMessage willMessage = willMessageStore
									.remove(session.getIdentifier());
							if (willMessage != null) {
								logger.info(
										"{} publish will message：identifier={}, remoteAddress={}",
										client.getSessionId(),
										session.getIdentifier(),
										client.getRemoteAddress());
								QosPublisher qosPublisher = QosPublisherFactory
										.makeQosPublisher(willMessage.getQos(),
												NettyMqttServer.this);
								qosPublisher.publish2Subscribers(willMessage);
							} else {
								logger.warn(
										"{} publish will message, but has no stored message：identifier={}, remoteAddress={}",
										client.getSessionId(),
										session.getIdentifier(),
										client.getRemoteAddress());
							}
						}
						if (!session.isCleanSessionFlag()) {
							// 保持会话
							logger.info(
									"{} hold session：identifier={}, remoteAddress={}",
									client.getSessionId(),
									session.getIdentifier(),
									client.getRemoteAddress());
						} else {
							// 删除会话
							subscriptionStore.unsub(client.allSubs());
							if (session.getSessionId().equals(
									client.getSessionId())) {
								RedisBatchProcUtil.submitDel(RedisKeyBuilder
										.buildSessionKey(session
												.getIdentifier()));
							}
							client.destroy();
						}
					}
				} else {
					logger.info(
							"{} disconnected: identifier={}, remoteAddress={}",
							client.getSessionId(), null,
							client.getRemoteAddress());
					subscriptionStore.unsub(client.allSubs());
					client.destroy();
				}
			}
		});

		namespace.addPingTimeoutListener(new PingTimeoutListener() {

			@Override
			public void onPingTimeout(Client client, int pingTimeout) {
				// TODO Auto-generated method stub
				if (client.incrementAndGetPingTimeoutCount() > 3) {
					client.disconnect();
					logger.warn(
							"3 times pingtimeout, disconnect {} ：dentifier={}，remoteAddress={}",
							client.getSessionId(), client.getSession()
									.getIdentifier(), client.getSession()
									.getRemoteAddress());
				}
			}
		});

		namespace.addEventListener(MqttMessageType.CONNECT.name(),
				MqttConnectMessage.class, new MqttConnenctHandler(this,
						namespace));
		namespace.addEventListener(MqttMessageType.PUBLISH.name(),
				MqttPublishMessage.class, new MqttPublishHandler(this));
		namespace.addEventListener(MqttMessageType.PUBREL.name(),
				MqttMessage.class, new MqttPubRelHandler(this));
		namespace.addEventListener(MqttMessageType.PUBREC.name(),
				MqttMessage.class, new MqttPubRecHandler(this));
		namespace.addEventListener(MqttMessageType.PUBCOMP.name(),
				MqttMessage.class, new MqttPubCompHandler(this));
		namespace.addEventListener(MqttMessageType.PUBACK.name(),
				MqttMessage.class, new MqttPubAckHandler(this));
		namespace.addEventListener(MqttMessageType.SUBSCRIBE.name(),
				MqttSubscribeMessage.class, new MqttSubscribeHandler(this));
		namespace.addEventListener(MqttMessageType.UNSUBSCRIBE.name(),
				MqttUnsubscribeMessage.class, new MqttUnsubscribeHandler(this));
		namespace.addEventListener(MqttMessageType.PINGREQ.name(),
				MqttMessage.class, new MqttPingHandler());
		namespace.addEventListener(MqttMessageType.DISCONNECT.name(),
				MqttMessage.class, new MqttDisconnectHandler());
		return namespace;
	}

	protected void applyConnectionOptions(ServerBootstrap bootstrap) {
		SocketConfig config = configuration.getSocketConfig();
		bootstrap.childOption(ChannelOption.TCP_NODELAY, config.isTcpNoDelay());
		if (config.getTcpSendBufferSize() != -1) {
			bootstrap.childOption(ChannelOption.SO_SNDBUF,
					config.getTcpSendBufferSize());
		}
		if (config.getTcpReceiveBufferSize() != -1) {
			bootstrap.childOption(ChannelOption.SO_RCVBUF,
					config.getTcpReceiveBufferSize());
		}
		// bootstrap.option(ChannelOption.ALLOCATOR,
		// PooledByteBufAllocator.DEFAULT);
		bootstrap.option(ChannelOption.ALLOCATOR,
				PooledByteBufAllocator.DEFAULT);
		// bootstrap.childOption(ChannelOption.ALLOCATOR,
		// PooledByteBufAllocator.DEFAULT);
		bootstrap.childOption(ChannelOption.ALLOCATOR,
				PooledByteBufAllocator.DEFAULT);
		bootstrap.childOption(ChannelOption.SO_KEEPALIVE,
				config.isTcpKeepAlive());
		bootstrap.option(ChannelOption.SO_LINGER, config.getSoLinger());
		bootstrap.option(ChannelOption.SO_REUSEADDR, config.isReuseAddress());
		bootstrap.option(ChannelOption.SO_BACKLOG, config.getAcceptBackLog());
	}

	protected SslContext createSslContext(Configuration configuration)
			throws Exception {
		if (configuration.getTrustStore() != null) {
			KeyStore ts = KeyStore.getInstance(configuration
					.getTrustStoreFormat());
			ts.load(configuration.getTrustStore(), configuration
					.getTrustStorePassword().toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(ts);
			// SslContext sslCtx =
			// SslContext.newServerContext(SslProvider.OPENSSL, );
		}
		return null;
	}

	protected SSLContext createSSLContext(Configuration configuration)
			throws Exception {
		TrustManager[] managers = null;
		if (configuration.getTrustStore() != null) {
			KeyStore ts = KeyStore.getInstance(configuration
					.getTrustStoreFormat());
			ts.load(configuration.getTrustStore(), configuration
					.getTrustStorePassword().toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(ts);
			managers = tmf.getTrustManagers();
		}
		KeyStore ks = KeyStore.getInstance(configuration.getKeyStoreFormat());
		ks.load(configuration.getKeyStore(), configuration
				.getKeyStorePassword().toCharArray());

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
				.getDefaultAlgorithm());
		kmf.init(ks, configuration.getKeyStorePassword().toCharArray());
		SSLContext serverContext = SSLContext.getInstance(configuration
				.getSSLProtocol());
		serverContext.init(kmf.getKeyManagers(), managers, null);
		return serverContext;
	}

	protected void initGroups() {
		if (configuration.isUseLinuxNativeEpoll()) {
			bossGroup = new EpollEventLoopGroup(configuration.getBossThreads());
			workerGroup = new EpollEventLoopGroup(
					configuration.getWorkerThreads());
		} else {
			bossGroup = new NioEventLoopGroup(configuration.getBossThreads());
			workerGroup = new NioEventLoopGroup(
					configuration.getWorkerThreads());
		}
	}

	@Override
	protected Configuration readConfiguration() {
		// TODO Auto-generated method stub
		InputStream is = null;
		Properties properties = null;
		try {
			URL url = NettyMqttServer.class
					.getResource('/' + mqttConfigureFile);
			try {
				is = new FileInputStream(URLDecoder.decode(url.getFile(),
						"UTF-8"));
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				logger.error(e1);
				return null;
			}
			properties = new Properties();
			try {
				properties.load(is);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error(e);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		Configuration config = new Configuration();
		String serverHost = properties.getProperty("mqtt.server.rpc.ip",
				"127.0.0.1");
		if (serverHost.equals("0.0.0.0")) {
			serverHost = NetUtils.getLocalHost();
		}
		config.setServerId(serverHost + ":"
				+ properties.getProperty("mqtt.server.rpc.port", "8888"));
		config.setHostname(properties.getProperty("mqtt.server.ip", "0.0.0.0"));
		config.setPort(Integer.parseInt(properties.getProperty(
				"mqtt.server.port", "9000")));
		config.setAllowAnonymous(Boolean.valueOf(properties.getProperty(
				"mqtt.proto.allowanonymous", "false")));
		config.setZeroByteClientId(Boolean.valueOf(properties.getProperty(
				"mqtt.proto.zerobyteclientid", "false")));
		config.setClusterType(ClusterType.valueOf(properties.getProperty(
				"mqtt.cluster.type", ClusterType.NONE.name())));
		config.setPingInterval(Integer.parseInt(properties.getProperty(
				"mqtt.server.ping.intvl", "120")));
		config.setPingTimeout(Integer.parseInt(properties.getProperty(
				"mqtt.server.ping.timeout", "120")));
		SocketConfig socketConfig = new SocketConfig();
		socketConfig.setTcpSendBufferSize(Integer.parseInt(properties
				.getProperty("mqtt.server.tcp.sendbuf", "4096")));
		socketConfig.setTcpReceiveBufferSize(Integer.parseInt(properties
				.getProperty("mqtt.server.tcp.rcvbuf", "4096")));
		config.setSocketConfig(socketConfig);
		Config redissonConfig = new Config();
		String[] redisServers = redis.getServers();
		String[] redisIpPortPasswd = StringDeal.split(redisServers[0], ":");
		String redisServer = "127.0.0.1:6379";
		String redisPasswd = "";
		boolean redisNeedAuth = false;
		if (redisIpPortPasswd.length == 1) {
			redisServer = redisIpPortPasswd[0] + ":" + "6379";
		} else if (redisIpPortPasswd.length == 2) {
			redisServer = redisIpPortPasswd[0] + ":" + redisIpPortPasswd[1];
		} else if (redisIpPortPasswd.length >= 3) {
			redisServer = redisIpPortPasswd[0] + ":" + redisIpPortPasswd[1];
			redisPasswd = redisIpPortPasswd[2];
			redisNeedAuth = true;
		}
		if (redisNeedAuth) {
			redissonConfig.useSingleServer().setAddress(redisServer)
					.setPassword(redisPasswd);
		} else {
			redissonConfig.useSingleServer().setAddress(redisServer);
		}
		// Instantiate Redisson connection
		RedissonClient redisson = Redisson.create(redissonConfig);
		// Instantiate RedissonClientStoreFactory
		RedissonStoreFactory redisStoreFactory = new RedissonStoreFactory(
				redisson);
		config.setStoreFactory(redisStoreFactory);
		return config;
	}
}
