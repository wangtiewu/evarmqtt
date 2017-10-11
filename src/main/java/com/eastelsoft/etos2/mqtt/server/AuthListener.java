package com.eastelsoft.etos2.mqtt.server;


public interface AuthListener {
	Session onAuth(String identifier, String userName, String password, String ip);
}
