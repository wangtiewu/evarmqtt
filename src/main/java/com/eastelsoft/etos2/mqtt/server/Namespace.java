package com.eastelsoft.etos2.mqtt.server;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.eastelsoft.etos2.mqtt.util.JsonSupport;
import com.eastelsoft.etos2.mqtt.util.XmlSupport;

/**
 * Fully thread-safe.
 *
 */
public interface Namespace {
	public static final String DEFAULT_NAME = "";
	String getName();
	Collection<Client> getAllClients();
	Client getClient(String sessionId);
	public Iterable<Client> getRoomClients(String room);
	public Set<String> getRooms(Client client);
	public void join(String room, String sessionId);
	public void leave(String room, String sessionId);
	/*will broardcast*/
	public void joinRoom(String room, String sessionId);
	/*will broardcast*/
	public void leaveRoom(String room, String sessionId);
	public boolean inRoom(String room, String sessionId);
	public int roomSessions(String room);
	public void onEvent(Client client, String eventName, Map parameters,
			Object arg);
	public void dispatch(String room, BaseMessage message);
	public JsonSupport getJsonSupport();
	public XmlSupport getXmlSupport();
	public int getIpAccessLimitCount();
	public int getTokenAccessLimitCount();
	public Queue<Listener> getListeners();
	public void setListeners(Queue<Listener> listeners);
	public void addListener(Listener listener);
	public void broadcast(BaseMessage message);
	public void broadcast(String room, BaseMessage message);
	public void onDisconnect(Client client);
	public void onConnect(Client client);
	<T> void addEventListener(String eventName, Class<T> eventClass,
			DataListener<T> listener);
	void onPingTimeout(Client client, int pingTimeout);
	void onException(Client client, Throwable cause);
	Session onAuth(String identifier, String userName, String password, String ip);
	void addConnectListener(ConnectListener connectListener);
	void addDisconnectListener(DisconnectListener disconnectListener);
	void addPingTimeoutListener(PingTimeoutListener pingTimeoutListener);
	void addAuthListener(AuthListener authListener);
}
