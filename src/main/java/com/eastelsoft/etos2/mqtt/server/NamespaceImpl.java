package com.eastelsoft.etos2.mqtt.server;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.eastelsoft.etos2.mqtt.auth.AuthService;
import com.eastelsoft.etos2.mqtt.cluster.PubSubType;
import com.eastelsoft.etos2.mqtt.cluster.StoreFactory;
import com.eastelsoft.etos2.mqtt.cluster.message.JoinLeaveMessage;
import com.eastelsoft.etos2.mqtt.util.JsonSupport;
import com.eastelsoft.etos2.mqtt.util.SchedulerKey;
import com.eastelsoft.etos2.mqtt.util.XmlSupport;

import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class NamespaceImpl implements Namespace {
	private static final int ACCESS_LIMIT_BASE = 720;
	private final static Logger logger = LoggerFactory
			.getLogger(NamespaceImpl.class);
	private final String name;// URI地址
	private final Map<String, Client> allClients = new ConcurrentHashMap<String, Client>();
	private final ConcurrentMap<String, Set<String>> roomClients = new ConcurrentHashMap<String, Set<String>>();
	private final ConcurrentMap<String, Set<String>> clientRooms = new ConcurrentHashMap<String, Set<String>>();
	private final Queue<ConnectListener> connectListeners = new ConcurrentLinkedQueue<ConnectListener>();
	private final Queue<DisconnectListener> disconnectListeners = new ConcurrentLinkedQueue<DisconnectListener>();
	private final ConcurrentMap<String, EventEntry<?>> eventListeners = new ConcurrentHashMap<String, EventEntry<?>>();
	private int ipAccLimCount;// 单ip访问单接口限制次数
	private int tokenAccLimCount;// 单token（用户）访问单接口限制次数
	private Queue<Listener> listeners = new ConcurrentLinkedQueue<Listener>();
	private final JsonSupport jsonSupport;
	private final XmlSupport xmlSupport;
	private final boolean broardcastJoinLeave;
	private final StoreFactory storeFactory;
	private AuthService authService;
	private PingTimeoutListener pingTimeoutListener;
	private MqttServer mqttServer;
	private AuthListener authListener;

	public NamespaceImpl(String name, Configuration configuration,
			MqttServer mqttServer) {
		super();
		this.name = name;
		this.ipAccLimCount = ACCESS_LIMIT_BASE * 30;
		this.tokenAccLimCount = ACCESS_LIMIT_BASE;
		this.jsonSupport = configuration.getJsonSupport();
		this.xmlSupport = configuration.getXmlSupport();
		this.broardcastJoinLeave = false;
		this.storeFactory = configuration.getStoreFactory();
		this.mqttServer = mqttServer;
	}

	public NamespaceImpl(String name, int ipAccLimCount,
			Configuration configuration, MqttServer nettyServer) {
		super();
		this.name = name;
		this.ipAccLimCount = ipAccLimCount;
		this.tokenAccLimCount = ACCESS_LIMIT_BASE;
		this.jsonSupport = configuration.getJsonSupport();
		this.xmlSupport = configuration.getXmlSupport();
		this.broardcastJoinLeave = false;
		this.storeFactory = configuration.getStoreFactory();
		this.mqttServer = mqttServer;
	}
	
	public NamespaceImpl(String name, int ipAccLimCount, boolean broardcastJoinLeave,
			Configuration configuration, MqttServer nettyServer) {
		super();
		this.name = name;
		this.ipAccLimCount = ipAccLimCount;
		this.tokenAccLimCount = ACCESS_LIMIT_BASE;
		this.jsonSupport = configuration.getJsonSupport();
		this.xmlSupport = configuration.getXmlSupport();
		this.broardcastJoinLeave = broardcastJoinLeave;
		this.storeFactory = configuration.getStoreFactory();
		this.mqttServer = mqttServer;
	}

	public String getName() {
		return name;
	}

	@Override
	public JsonSupport getJsonSupport() {
		// TODO Auto-generated method stub
		return jsonSupport;
	}

	@Override
	public XmlSupport getXmlSupport() {
		return xmlSupport;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Namespace other = (Namespace) obj;
		if (name == null) {
			if (other.getName() != null)
				return false;
		} else if (!name.equals(other.getName()))
			return false;
		return true;
	}

	private <K, V> void join(ConcurrentMap<K, Set<V>> map, K key, V value) {
		Set<V> clients = map.get(key);
		if (clients == null) {
			clients = Collections
					.newSetFromMap(new ConcurrentHashMap<V, Boolean>());
			Set<V> oldClients = map.putIfAbsent(key, clients);
			if (oldClients != null) {
				clients = oldClients;
			}
		}
		clients.add(value);
		// object may be changed due to other concurrent call
		if (clients != map.get(key)) {
			// re-join if queue has been replaced
			join(map, key, value);
		}
	}

	public void join(String room, String sessionId) {
		join(roomClients, room, sessionId);
		join(clientRooms, sessionId, room);
		mqttServer.getScheduler().cancel(new SchedulerKey(
				SchedulerKey.Type.PING_TIMEOUT, sessionId));
	}

	public void joinRoom(String room, String sessionId) {
		join(room, sessionId);
		if (broardcastJoinLeave) {
		storeFactory.pubSubStore().publish(PubSubType.JOIN,
				new JoinLeaveMessage(sessionId, room, getName()));
		}
	}

	@Override
	public int roomSessions(String room) {
		// TODO Auto-generated method stub
		Set<String> sessions = roomClients.get(room);
		return sessions == null ? 0 : sessions.size();
	}

	private <K, V> void leave(ConcurrentMap<K, Set<V>> map, K room, V sessionId) {
		Set<V> clients = map.get(room);
		if (clients == null) {
			return;
		}
		clients.remove(sessionId);

		if (clients.isEmpty()) {
			map.remove(room, Collections.emptySet());
		}
	}

	public void leaveRoom(String room, String sessionId) {
		leave(room, sessionId);
		if (broardcastJoinLeave) {
		storeFactory.pubSubStore().publish(PubSubType.LEAVE,
				new JoinLeaveMessage(sessionId, room, getName()));
		}
	}

	public void leave(String room, String sessionId) {
		leave(roomClients, room, sessionId);
		leave(clientRooms, sessionId, room);
	}

	@Override
	public boolean inRoom(String room, String sessionId) {
		// TODO Auto-generated method stub
		Set<String> rooms = clientRooms.get(sessionId);
		if (rooms == null) {
			return false;
		}
		return rooms.contains(room);
	}

	public Set<String> getRooms(Client client) {
		Set<String> res = clientRooms.get(client.getSessionId());
		if (res == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(res);
	}

	public Iterable<Client> getRoomClients(String room) {
		Set<String> sessionIds = roomClients.get(room);

		if (sessionIds == null) {
			return Collections.emptyList();
		}

		List<Client> result = new ArrayList<Client>();
		for (String sessionId : sessionIds) {
			Client client = allClients.get(sessionId);
			if (client != null) {
				result.add(client);
			}
		}
		return result;
	}

	public Collection<Client> getAllClients() {
		return Collections.unmodifiableCollection(allClients.values());
	}

	public Client getClient(String uuid) {
		if (uuid == null) {
			return null;
		}
		return allClients.get(uuid);
	}

	@Override
	public void onDisconnect(Client client) {
		// TODO Auto-generated method stub
		for(DisconnectListener listener : disconnectListeners) {
			listener.onDisconnect(client);
		}
		allClients.remove(client.getSessionId());
		leaveRoom(getName(), client.getSessionId());
		Set<String> allRooms = getRooms(client);
		for (String room : allRooms) {
			leaveRoom(room, client.getSessionId());
		}
	}

	@Override
	public void onConnect(Client client) {
		// TODO Auto-generated method stub
		allClients.put(client.getSessionId(), client);
		joinRoom(getName(), client.getSessionId());
		for(ConnectListener listener : connectListeners) {
			listener.onConnect(client);
		}
	}

	@Override
	public int getIpAccessLimitCount() {
		return ipAccLimCount;
	}

	@Override
	public int getTokenAccessLimitCount() {
		// TODO Auto-generated method stub
		return tokenAccLimCount;
	}

	/**
	 * 获取线程局部变量requestData
	 * 
	 * @return
	 */
	// public static ThreadLocal<Object> getRequestData() {
	// return requestData;
	// }

	public Queue<Listener> getListeners() {
		return listeners;
	}

	public void setListeners(Queue<Listener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public void addListener(Listener listener) {
		// TODO Auto-generated method stub
		listeners.add(listener);
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
					e.printStackTrace();
					return 0;
				}
			}
		}
		return 0;
	}

	@Override
	public void dispatch(String room, BaseMessage message) {
		// TODO Auto-generated method stub
		Iterable<Client> clients = getRoomClients(room);

		for (Client client : clients) {
			client.send(name, message);
		}
	}

	@Override
	public void broadcast(BaseMessage message) {
		BroadcastOperations broadcastOperations = new BroadcastOperations(this,
				name, storeFactory);
		broadcastOperations.sendEvent(name, message);
	}

	@Override
	public void broadcast(String room, BaseMessage message) {
		BroadcastOperations broadcastOperations = new BroadcastOperations(this,
				room, storeFactory);
		broadcastOperations.sendEvent(name, message);
	}

	@Override
	public void onPingTimeout(Client client, int pingTimeout) {
		// TODO Auto-generated method stub
		if (pingTimeoutListener != null) {
			pingTimeoutListener.onPingTimeout(client, pingTimeout);
		}
	}
	
	@Override
	public void onException(Client client, Throwable cause) {
		
	}

	@Override
	public void addPingTimeoutListener(PingTimeoutListener pingTimeoutListener) {
		// TODO Auto-generated method stub
		this.pingTimeoutListener = pingTimeoutListener;
	}

	@Override
	public Session onAuth(String identifier, String userName, String password, String ip) {
		// TODO Auto-generated method stub
		// namespace上配置的认证优先
		if (authListener != null) {
			return authListener.onAuth(identifier, userName, password, ip);
		} else {
			return null;
		}
	}

	@Override
	public void addAuthListener(AuthListener authListener) {
		// TODO Auto-generated method stub
		this.authListener = authListener;
	}
	
	private AuthService getAuthService() {
		if (authService == null) {
			authService = (AuthService) EvarBeanFactory.instance().makeBean(
					"authService");
		}
		return authService;
	}

	@Override
	public void onEvent(Client client, String eventName, Map parameters,
			Object arg) {
		// TODO Auto-generated method stub
		client.resetPingTimeoutCount();
		EventEntry entry = eventListeners.get(eventName);
		if (entry == null) {
			logger.error("根据事件名称 {0} 找不到EventEntry", eventName);
			BaseRespMessage baseRespMessage = new BaseRespMessage();
			baseRespMessage.setEcode(ErrorCode.ECODE_SYSFAIL);
			baseRespMessage.setEmsg(ErrorCode.getEmsg(ErrorCode.ECODE_SYSFAIL));
			client.send(eventName, baseRespMessage);
			return;
		}
		try {
			Queue<DataListener> listeners = entry.getListeners();
			for (DataListener dataListener : listeners) {
				dataListener.onData(client, parameters, arg);
			}
		} catch (MyException e) {
			// logger.warn(e);
			client.send(eventName,
					new BaseRespMessage(e.getEcode(), e.getEmsg()));
			return;
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			BaseRespMessage baseRespMessage = new BaseRespMessage();
			baseRespMessage.setEcode(ErrorCode.ECODE_SYSFAIL);
			baseRespMessage.setEmsg(ErrorCode.getEmsg(ErrorCode.ECODE_SYSFAIL));
			client.send(eventName, baseRespMessage);
			return;
		}
	}

	@Override
	public <T> void addEventListener(String eventName, Class<T> eventClass,
			DataListener<T> listener) {
		// TODO Auto-generated method stub
		EventEntry entry = eventListeners.get(eventName);
		if (entry == null) {
			entry = new EventEntry(eventClass);
			EventEntry<?> oldEntry = eventListeners.putIfAbsent(eventName,
					entry);
			if (oldEntry != null) {
				entry = oldEntry;
			}
		}
		entry.addListener(listener);
	}

	@Override
	public void addConnectListener(ConnectListener connectListener) {
		// TODO Auto-generated method stub
		this.connectListeners.add(connectListener);
	}

	@Override
	public void addDisconnectListener(DisconnectListener disconnectListener) {
		// TODO Auto-generated method stub
		this.disconnectListeners.add(disconnectListener);
	}

}
