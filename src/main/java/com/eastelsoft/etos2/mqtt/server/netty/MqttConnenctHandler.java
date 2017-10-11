package com.eastelsoft.etos2.mqtt.server.netty;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.eastelsoft.etos2.mqtt.auth.AuthConsts.CAPABILITY;
import com.eastelsoft.etos2.mqtt.auth.AuthReq;
import com.eastelsoft.etos2.mqtt.auth.AuthResp;
import com.eastelsoft.etos2.mqtt.auth.AuthService;
import com.eastelsoft.etos2.mqtt.cluster.MqttNotifierHelper;
import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.Client.ConnectionCloseType;
import com.eastelsoft.etos2.mqtt.server.ErrorCode;
import com.eastelsoft.etos2.mqtt.server.MqttConsts;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.Namespace;
import com.eastelsoft.etos2.mqtt.server.RedisKeyBuilder;
import com.eastelsoft.etos2.mqtt.server.Session;
import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.mqtt.server.Subscription;
import com.eastelsoft.etos2.mqtt.server.SubscriptionStore;
import com.eastelsoft.etos2.mqtt.server.Topic;
import com.eastelsoft.etos2.mqtt.server.WillMessageStore;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;

import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.IdGenerator;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttConnenctHandler extends MqttMessageHandler<MqttConnectMessage> {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttConnenctHandler.class);
	private final AuthService authService = (AuthService) EvarBeanFactory
			.instance().makeBean("authService");
	private final Redis redis = (Redis) EvarBeanFactory.instance().makeBean(
			"redis");
	private final WillMessageStore willMessageStore = (WillMessageStore) EvarBeanFactory
			.instance().makeBean("willMessageStore");
	private final SubscriptionStore subscriptionStore = (SubscriptionStore) EvarBeanFactory
			.instance().makeBean("subscriptionStore");
	private MqttServer mqttServer;
	private Namespace namespace;

	public MqttConnenctHandler(MqttServer mqttServer, Namespace namespace) {
		this.mqttServer = mqttServer;
		this.namespace = namespace;
	}

	@Override
	public void onData(Client client, Map<String, List<String>> parameters,
			MqttConnectMessage data) throws Exception {
		// TODO Auto-generated method stub
		IntfLoggerUtil.logInboundMessage(logger,
				MqttMessageType.CONNECT.name(), client, data.toString());
		MqttConnectPayload payload = data.payload();
		String identifier = payload.clientIdentifier();
		String userName = payload.userName();
		String password = payload.password();
		MqttConnAckMessage connAck;
		if (data.variableHeader().version() != MqttVersion.MQTT_3_1
				.protocolLevel()
				&& data.variableHeader().version() != MqttVersion.MQTT_3_1_1
						.protocolLevel()) {
			// Protocol Level unacceptable
			connAck = makeConnAck(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
			client.send(MqttMessageType.CONNACK.name(), connAck);
			IntfLoggerUtil.logOutboundMessage(logger,
					MqttMessageType.CONNACK.name(), client, connAck.toString());
			client.disconnect();
			return;
		}
		// check identifier
		boolean newIdentifier = false;
		boolean cleanSession = data.variableHeader().isCleanSession();
		if (StringUtils.isEmpty(identifier)) {
			if (!cleanSession
					|| !mqttServer.getConfiguration().isZeroByteClientId()) {
				connAck = makeConnAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED);
				client.send(MqttMessageType.CONNACK.name(), connAck);
				IntfLoggerUtil.logOutboundMessage(logger,
						MqttMessageType.CONNACK.name(), client,
						connAck.toString());
				client.disconnect();
				return;
			}
			identifier = IdGenerator.createObjectIdHex();
			newIdentifier = true;
			logger.info(
					"{} generated an identifier：identifier={}，remoteAddress={}",
					client.getSessionId(), identifier,
					client.getRemoteAddress());
		}
		// login
		if (!login(client, userName, password, identifier)) {
			client.disconnect();
			return;
		}
		// clearn session flag
		boolean sessionPresent = false;
		if (newIdentifier) {
			// do nothing
		} else {
			if (cleanSession) {
				Session existedSession = redis.get(
						RedisKeyBuilder.buildSessionKey(identifier),
						Session.class);
				if (existedSession != null) {
					if (existedSession.getServerId().equals(
							mqttServer.getServerId())) {
						Client existedClient = namespace
								.getClient(existedSession.getSessionId());
						if (existedClient != null) {
							existedClient
									.assignConnCloseType(ConnectionCloseType.DUP_CONNECT);
							existedClient.disconnect();
							logger.info("{} 踢下线：dentifier={}，remoteAddress={}",
									existedSession.getSessionId(),
									existedSession.getIdentifier(),
									existedSession.getRemoteAddress());
						}
					} else {
						// 通知其他节点断开连接
						MqttNotifierHelper.disconnect(
								existedSession.getServerId(),
								existedSession.getSessionId());
						logger.info(
								"通知节点 {} 的客户端下线：sessionId={}，dentifier={}，remoteAddress={}",
								existedSession.getServerId(),
								existedSession.getSessionId(),
								existedSession.getIdentifier(),
								existedSession.getRemoteAddress());
					}
				}
			} else {
				Session existedSession = redis.get(
						RedisKeyBuilder.buildSessionKey(identifier),
						Session.class);
				if (existedSession != null) {
					// 断开之前的连接
					if (existedSession.getServerId().equals(
							mqttServer.getServerId())) {
						Client existedClient = namespace
								.getClient(existedSession.getSessionId());
						if (existedClient != null) {
							// 之前的订阅topics保持不变
							Set<Subscription> oldSubs = existedClient.allSubs();
							Set<Subscription> copyedSubs = new HashSet();
							for (Subscription oldSub : oldSubs) {
								Subscription sub = new Subscription(
										client.getSessionId(), client
												.getSession().getIdentifier(),
										oldSub.getTopic(), oldSub.getQos(),
										oldSub.getServerId());
								copyedSubs.add(sub);
							}
							client.addSubs(copyedSubs);
							subscriptionStore.sub(copyedSubs);
							existedClient
									.assignConnCloseType(ConnectionCloseType.DUP_CONNECT);
							existedClient.disconnect();
							logger.info("{} 踢下线：dentifier={}，remoteAddress={}",
									existedSession.getSessionId(),
									existedSession.getIdentifier(),
									existedSession.getRemoteAddress());
						}
					} else {
						// 通知其他节点断开连接
						MqttNotifierHelper.disconnect(
								existedSession.getServerId(),
								existedSession.getSessionId());
						logger.info(
								"通知节点 {} 的客户端下线：sessionId={}，dentifier={}，remoteAddress={}",
								existedSession.getServerId(),
								existedSession.getSessionId(),
								existedSession.getIdentifier(),
								existedSession.getRemoteAddress());
					}
					sessionPresent = true;
				}
			}
		}
		// keep alive
		int keepAliveSeconds = setKeepAlive(client, data);
		// will message
		client.getSession().setWillMessageFlag(storeWillMessage(client, data));
		// 保存session到redis中
		client.getSession().setCleanSessionFlag(cleanSession);
		namespace.joinRoom(client.getSession().getIdentifier(),
				client.getSessionId());
		client.lastTtlTime((int) (System.currentTimeMillis() / 1000));
		redis.set(RedisKeyBuilder.buildSessionKey(identifier),
				client.getSession(), 1000L * keepAliveSeconds
						* MqttConsts.SESSION_TTL_TIMES);
		connAck = makeConnAck(CONNECTION_ACCEPTED, sessionPresent);
		client.send(MqttMessageType.CONNACK.name(), connAck);
		IntfLoggerUtil.logOutboundMessage(logger,
				MqttMessageType.CONNACK.name(), client, connAck.toString());
	}

	private boolean storeWillMessage(Client client, MqttConnectMessage data) {
		// TODO Auto-generated method stub
		if (data.variableHeader().isWillFlag()) {
			MqttQoS willQos = MqttQoS.valueOf(data.variableHeader().willQos());
			String willTopic = data.payload().willTopic();
			Topic topic = new Topic(willTopic);
			if (!topic.isValid()) {
				logger.error(
						"{Will topic {} is not valid，sessionId={}，identifier={}，remoteAddress={}",
						topic, client.getSessionId(), client.getSession()
								.getIdentifier(), client.getRemoteAddress());
				return false;
			} else if (topic.containWildcard()) {
				logger.error(
						"{Will topic {} contain wildcard，sessionId={}，identifier={}，remoteAddress={}",
						topic, client.getSessionId(), client.getSession()
								.getIdentifier(), client.getRemoteAddress());
				return false;
			}
			String willMessage = data.payload().willMessage();
			if (StringUtils.isEmpty(willTopic)
					|| StringUtils.isEmpty(willMessage)) {
				logger.error(
						"{} 设置了willFlag，但未设置 willTopic或willMessage：remoteAddress={}",
						client.getSessionId(), client.getRemoteAddress());
				return false;
			}
			if (willQos == null) {
				logger.error("{} willQos值 {} 非法：remoteAddress={}",
						client.getSessionId(), data.variableHeader().willQos(),
						client.getRemoteAddress());
				return false;
			}
			StoredMessage willMsg = new StoredMessage(topic,
					willMessage.getBytes(), data.variableHeader()
							.isWillRetain(), data.variableHeader().willQos(),
					mqttServer.getServerId());
			willMessageStore.put(client.getSession().getIdentifier(), willMsg);
			return true;
		}
		return false;
	}

	private int setKeepAlive(Client client, MqttConnectMessage data) {
		// TODO Auto-generated method stub
		int keepAliveSeconds = data.variableHeader().keepAliveTimeSeconds();
		if (keepAliveSeconds > 0) {
			client.setKeepAliveSeconds((int) (keepAliveSeconds * 1.2));
			return (int) (keepAliveSeconds * 1.2);
		} else {
			// 默认按配置文件中的值
			client.setKeepAliveSeconds(mqttServer.getConfiguration()
					.getPingInterval());
			return mqttServer.getConfiguration().getPingInterval();
		}
	}

	private MqttConnAckMessage makeConnAck(MqttConnectReturnCode code) {
		return makeConnAck(code, false);
	}

	private MqttConnAckMessage makeConnAck(MqttConnectReturnCode code,
			boolean sessionPresent) {
		MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
				MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(
				code, sessionPresent);
		MqttConnAckMessage connAck = new MqttConnAckMessage(mqttFixedHeader,
				mqttConnAckVariableHeader);
		return connAck;
	}

	private boolean login(Client client, String userName, String password,
			String identifier) {
		if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)) {
			if (mqttServer.getConfiguration().isAllowAnonymous()) {
				Session session = new Session(identifier, "anonymous",
						mqttServer.getServerId(), client.getSessionId());
				session.setRemoteAddress(client.getRemoteAddress() == null ? ""
						: client.getRemoteAddress().toString());
				client.setSession(session);
				return true;
			} else {
				MqttConnAckMessage connAck = makeConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
				client.send(MqttMessageType.CONNACK.name(), connAck);
				IntfLoggerUtil.logOutboundMessage(logger,
						MqttMessageType.CONNACK.name(), client,
						connAck.toString());
				return false;
			}
		}
		AuthReq authReq = new AuthReq(userName, password,
				CAPABILITY.MQTT.name());
		AuthResp authResp = authService.auth(authReq, "");
		if (authResp.getEcode().equals(ErrorCode.ECODE_SUCCESS)) {
			Session session = new Session(identifier, userName,
					mqttServer.getServerId(), client.getSessionId());
			session.setRemoteAddress(client.getRemoteAddress() == null ? ""
					: client.getRemoteAddress().toString());
			client.setSession(session);
			return true;
		} else {
			MqttConnAckMessage connAck = makeConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
			client.send(MqttMessageType.CONNACK.name(), connAck);
			IntfLoggerUtil.logOutboundMessage(logger,
					MqttMessageType.CONNACK.name(), client, connAck.toString());
			return false;
		}
	}
}
