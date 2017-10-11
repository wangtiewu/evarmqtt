package com.eastelsoft.etos2.mqtt.server;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.eastelsoft.etos2.mqtt.server.MqttConsts.QoS;
import com.eastelsoft.etos2.mqtt.util.JacksonUtils;
import com.eastelsoft.etos2.mqtt.util.RedisBatchProcUtil;
import com.eastelsoft.etos2.mqtt.util.SchedulerKey;

import eet.evar.StringDeal;
import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class NamespaceClient implements Client {
	private final Logger logger = LoggerFactory
			.getLogger(NamespaceClient.class);
	private final TransportClient transportClient;
	private final Namespace namespace;
	private final MqttServer mqttServer;
	private static final Redis redis = (Redis) EvarBeanFactory.instance()
			.makeBean("redis");
	private Session session;
	private final AtomicReference<ConnectionCloseType> connCloseType = new AtomicReference<ConnectionCloseType>();
	private Map<Integer, StoredMessage> inboundFlightMessageMap;
	private Map<Integer, StoredMessage> outboundFlightMessageMap;
	private Queue<StoredMessage> messageQueue;
	private Set<Subscription> subs;
	private AtomicLong messageIdCreater = new AtomicLong();
	private volatile boolean destroyed = false;
	private int keepAliveSeconds = 10;
	private int lastKeepAliveTime;
	private int lastTtlTime;
	private int lastOutboundTime;
	private AtomicInteger pingTimeoutCount = new AtomicInteger(0);

	public NamespaceClient(TransportClient transportClient,
			Namespace namespace, MqttServer mqttServer) {
		this.namespace = namespace;
		this.transportClient = transportClient;
		this.mqttServer = mqttServer;
	}

	public TransportClient getTransportClient() {
		return transportClient;
	}

	@Override
	public void destroy() {
		disconnect();
		if (inboundFlightMessageMap != null) {
			inboundFlightMessageMap.clear();
			inboundFlightMessageMap = null;
		}
		if (outboundFlightMessageMap != null) {
			outboundFlightMessageMap.clear();
			outboundFlightMessageMap = null;
		}
		if (messageQueue != null) {
			messageQueue.clear();
			messageQueue = null;
		}
		if (subs != null) {
			subs.clear();
			subs = null;
		}
		destroyed = true;
		logger.info("Client destroyed:{}", toString());
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public boolean isTransportActive() {
		if (transportClient != null) {
			return transportClient.isActive();
		}
		return false;
	}

	@Override
	public void send(String eventName, BaseMessage data) {
		this.lastOutboundTime = (int)(System.currentTimeMillis()/1000);
		this.pingTimeoutCount.set(0);
		tryTtlSession();
		transportClient.send(data);
	}

	@Override
	public void send(String eventName, Object data) {
		this.lastOutboundTime = (int)(System.currentTimeMillis()/1000);
		this.pingTimeoutCount.set(0);
		tryTtlSession();
		transportClient.send(data);
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		if (transportClient != null) {
			transportClient.disconnect();
		}

	}

	@Override
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
		if (transportClient != null) {
			transportClient.setKeepAliveSeconds(keepAliveSeconds);
		}
	}

	@Override
	public int getKeepAliveSeconds() {
		// TODO Auto-generated method stub
		return this.keepAliveSeconds;
	}

	@Override
	public void lastKeepAliveTime(int pingtime) {
		// TODO Auto-generated method stub
		this.lastKeepAliveTime = pingtime;
	}

	@Override
	public int getLastKeepAliveTime() {
		// TODO Auto-generated method stub
		return this.lastKeepAliveTime;
	}

	@Override
	public void lastTtlTime(int ttlTime) {
		// TODO Auto-generated method stub
		this.lastTtlTime = ttlTime;
	}

	@Override
	public int getLastTtlTime() {
		// TODO Auto-generated method stub
		return this.lastTtlTime;
	}

	@Override
	public Namespace getNamespace() {
		// TODO Auto-generated method stub
		return namespace;
	}

	@Override
	public String getSessionId() {
		// TODO Auto-generated method stub
		if (transportClient != null) {
			return transportClient.getSessionId();
		}
		return null;
	}

	@Override
	public Session getSession() {
		// TODO Auto-generated method stub
		return session;
	}

	@Override
	public void setSession(Session session) {
		// TODO Auto-generated method stub
		this.session = session;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		return transportClient != null ? transportClient.getRemoteAddress()
				: null;
	}

	@Override
	public void joinRoom(String room) {
		// TODO Auto-generated method stub
		if (transportClient != null) {
			namespace.joinRoom(room, transportClient.getSessionId());
		}
	}

	@Override
	public void leaveRoom(String room) {
		// TODO Auto-generated method stub
		if (transportClient != null) {
			namespace.leaveRoom(room, transportClient.getSessionId());
		}
	}

	@Override
	public Set<String> getAllRooms() {
		// TODO Auto-generated method stub
		return namespace.getRooms(this);
	}

	@Override
	public String getRemoteIp() {
		return transportClient != null ? transportClient.getRemoteIp() : null;
	}

	@Override
	public boolean assignConnCloseType(ConnectionCloseType closeType) {
		boolean success = connCloseType.compareAndSet(null, closeType);
		if (!success) {
			logger.error("设置 {} 的关闭类型失败，当前值={}，期望值={}，remoteAddress={}",
					getSessionId(), connCloseType.get(), closeType,
					getRemoteAddress());
		}
		return success;
	}

	@Override
	public ConnectionCloseType getConnCloseType() {
		return this.connCloseType.get();
	}

	@Override
	public void addInboundFlightMessage(final int messageId, final StoredMessage message) {
		if (inboundFlightMessageMap == null) {
			synchronized (this) {
				if (inboundFlightMessageMap == null) {
					inboundFlightMessageMap = new ConcurrentHashMap<>();
				}
			}
		}
		inboundFlightMessageMap.put(messageId, message);
		mqttServer.getScheduler().schedule(
				new SchedulerKey(SchedulerKey.Type.QOS2_WAIT_PUBREL,
						getSessionId() + ":" + messageId),
				new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (removeInboundFlightMessage(messageId) != null) {
							logger.error(
									"{Publish topic {} fail：PUBREL wait timeout={}seconds，sessionId={}，identifier={}，remoteAddress={}，messageId={}",
									message.getTopic(),
									MqttConsts.WaitTimeOut.QOS2_PUBREL_TIMEOUT
											.value(), getSessionId(),
									getSession().getIdentifier(),
									getRemoteAddress(), messageId);
						}
					}
				}, MqttConsts.WaitTimeOut.QOS2_PUBREL_TIMEOUT.value(),
				TimeUnit.SECONDS);
	}

	@Override
	public StoredMessage removeInboundFlightMessage(int messageId) {
		if (inboundFlightMessageMap != null) {
			return inboundFlightMessageMap.remove(messageId);
		}
		return null;
	}

	@Override
	public int createMessageId() {
		if (outboundFlightMessageMap == null) {
			return (int) (messageIdCreater.incrementAndGet() % 0xFFFF);
		}
		int nextMsgId = (int) (messageIdCreater.incrementAndGet() % 0xFFFF);
		final int msgId = nextMsgId;
		while (outboundFlightMessageMap.containsKey(nextMsgId)) {
			nextMsgId = (int) (messageIdCreater.incrementAndGet() % 0xFFFF);
			if (msgId == nextMsgId) {
				break;
			}
		}
		return nextMsgId;
	}

	@Override
	public void addOutboundFlightMessage(int messageId, StoredMessage message) {
		if (outboundFlightMessageMap == null) {
			synchronized (this) {
				if (outboundFlightMessageMap == null) {
					outboundFlightMessageMap = new ConcurrentHashMap<>();
				}
			}
		}
		outboundFlightMessageMap.put(messageId, message);
	}

	@Override
	public StoredMessage removeOutboundFlightMessage(int messageId) {
		if (outboundFlightMessageMap != null) {
			return outboundFlightMessageMap.remove(messageId);
		}
		return null;
	}

	@Override
	public StoredMessage getOutboundFlightMessage(int messageId) {
		if (outboundFlightMessageMap != null) {
			return outboundFlightMessageMap.get(messageId);
		}
		return null;
	}

	@Override
	public void pushMessage(StoredMessage message) {
		if (messageQueue == null) {
			synchronized (this) {
				if (messageQueue == null) {
					messageQueue = new ConcurrentLinkedDeque<StoredMessage>();
				}
			}
		}
		messageQueue.offer(message);
	}

	@Override
	public StoredMessage popMessage() {
		if (messageQueue != null) {
			return messageQueue.poll();
		}
		return null;
	}

	@Override
	public void sub(Subscription subscription) {
		// TODO Auto-generated method stub
		if (subs == null) {
			synchronized (this) {
				if (subs == null) {
					subs = Collections
							.newSetFromMap(new ConcurrentHashMap<Subscription, Boolean>());
				}
			}
		}
		subs.add(subscription);
	}

	@Override
	public void unsub(Subscription subscription) {
		// TODO Auto-generated method stub
		if (subs != null) {
			subs.remove(subscription);
		}
	}

	@Override
	public Set<Subscription> allSubs() {
		// TODO Auto-generated method stub
		return subs == null ? Collections.EMPTY_SET : subs;
	}

	@Override
	public void addSubs(Set<Subscription> toAddSubs) {
		// TODO Auto-generated method stub
		if (toAddSubs == null) {
			return;
		}
		if (subs != null) {
			subs.addAll(toAddSubs);
		} else {
			for (Subscription sub : toAddSubs) {
				sub(sub);
			}
		}
	}

	@Override
	public void clearSubs() {
		if (subs != null) {
			subs.clear();
			subs = null;
		}
	}

	@Override
	public int getPingTimeoutCount() {
		// TODO Auto-generated method stub
		return this.pingTimeoutCount.get() ;
	}

	@Override
	public void resetPingTimeoutCount() {
		// TODO Auto-generated method stub
		this.pingTimeoutCount.set(0);
	}
	
	@Override
	public int incrementAndGetPingTimeoutCount() {
		return this.pingTimeoutCount.incrementAndGet();
	}

	private void writeLog(String eventName, BaseMessage data) {
		String cmdFlow = "CLOUD->*";

		if (logger.isInfoEnabled()) {
			logger.info(eventName + "_resp(" + cmdFlow + "): "
					+ JacksonUtils.bean2Json(data));
		}
	}

	private void writeLog(String eventName, Object object) {
		String cmdFlow = "CLOUD->*";
		if (logger.isInfoEnabled()) {
			logger.info(eventName + "_resp(" + cmdFlow + "): "
					+ JacksonUtils.bean2Json(object));
		}
	}

	private void writeLog(String eventName, int statusCode) {
		String cmdFlow = "CLOUD->APP";
		logger.info(eventName + "_resp(" + cmdFlow + "): statusCode="
				+ statusCode);
	}

	@Override
	public Transport getTransport() {
		// TODO Auto-generated method stub
		return transportClient != null ? transportClient.getTransport()
				: Transport.UNKNOWN;
	}

	@Override
	public String toString() {
		return StringDeal.format(
				"[sessionId: {}, identifier: {}, remoteAddress: {}]",
				new String[] {
						getSessionId(),
						getSession() == null ? "" : getSession()
								.getIdentifier(),
						getRemoteAddress() == null ? "" : getRemoteAddress()
								.toString() });
	}

	private void tryTtlSession() {
		// TODO Auto-generated method stub
		if (getSession() == null) {
			return;
		}
		if (getKeepAliveSeconds() * MqttConsts.SESSION_TTL_TIMES
				- (lastOutboundTime - getLastTtlTime()) < 2 * getKeepAliveSeconds()) {
			// session ttl
			lastTtlTime((int)(System.currentTimeMillis()/1000));
			RedisBatchProcUtil.submitTtt(RedisKeyBuilder.buildSessionKey(getSession()
					.getIdentifier()), 1000L * getKeepAliveSeconds()
					* MqttConsts.SESSION_TTL_TIMES);
		}
	}
}
