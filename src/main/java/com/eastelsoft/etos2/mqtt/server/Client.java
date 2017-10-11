package com.eastelsoft.etos2.mqtt.server;

import java.net.SocketAddress;
import java.util.Set;

public interface Client {
	public enum ConnectionCloseType {
		DUP_CONNECT,// 因为重复登陆，服务端关闭老连接
		CLIENT_CLOSED,// 客户端关闭连接（包括异常断开）
		PROTO_DISCONNECTED,// 收到Disconnect消息
	}
	
	public enum Transport {
	    UNKNOWN("unknown"),
		TCP("tcp"),
	    WEBSOCKET("websocket");

	    private final String value;

	    Transport(String value) {
	        this.value = value;
	    }

	    public String getValue() {
	        return value;
	    }

	    public static Transport byName(String value) {
	        for (Transport t : Transport.values()) {
	            if (t.getValue().equals(value)) {
	                return t;
	            }
	        }
	        throw new IllegalArgumentException("Can't find " + value + " transport");
	    }

	}
	
	void destroy();
	boolean isDestroyed();
	boolean isTransportActive();
	String getRemoteIp();
	void send(String eventName, BaseMessage data);
	void send(String eventName, Object data);
	void disconnect();
	Namespace getNamespace();
	String getSessionId();
	Session getSession();
	void setSession(Session session);
	SocketAddress getRemoteAddress();
	void joinRoom(String room);
	void leaveRoom(String room);
	Set<String> getAllRooms();
	public TransportClient getTransportClient();
	Transport getTransport();
	void setKeepAliveSeconds(int keepAliveSeconds);
	int getKeepAliveSeconds();
	void lastKeepAliveTime(int pingtime);
	int getLastKeepAliveTime();
	void lastTtlTime(int ttlTime);
	int getLastTtlTime();
	boolean assignConnCloseType(ConnectionCloseType closeType);
	ConnectionCloseType getConnCloseType();
	void addInboundFlightMessage(int messageId, StoredMessage message);
	StoredMessage removeInboundFlightMessage(int messageId);
	void addOutboundFlightMessage(int messageId, StoredMessage message);
	StoredMessage removeOutboundFlightMessage(int messageId);
	StoredMessage getOutboundFlightMessage(int messageId);
	int createMessageId();
	void pushMessage(StoredMessage message);
	StoredMessage popMessage();
	void sub(Subscription subscription);
	void unsub(Subscription subscription);
	Set<Subscription> allSubs();
	void addSubs(Set<Subscription> subscriptions);
	void clearSubs();
	int getPingTimeoutCount();
	void resetPingTimeoutCount();
	int incrementAndGetPingTimeoutCount();
}
