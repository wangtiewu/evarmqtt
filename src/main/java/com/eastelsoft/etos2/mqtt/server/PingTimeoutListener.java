package com.eastelsoft.etos2.mqtt.server;


public interface PingTimeoutListener {
	public void onPingTimeout(Client client, int pingTimeout);
}
