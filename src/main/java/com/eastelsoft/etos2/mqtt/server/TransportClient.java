package com.eastelsoft.etos2.mqtt.server;

import java.net.SocketAddress;

import com.eastelsoft.etos2.mqtt.server.Client.Transport;

public interface TransportClient {

	void disconnect();

	String getSessionId();

	SocketAddress getRemoteAddress();

	String getRemoteIp();

	Transport getTransport();
	
	void send(Object data);

	void setKeepAliveSeconds(int keepAliveSeconds);
	
	boolean isActive();

}
