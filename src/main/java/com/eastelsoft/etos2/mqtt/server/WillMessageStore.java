package com.eastelsoft.etos2.mqtt.server;

public interface WillMessageStore {

	public void put(String identifier, StoredMessage willMessage);

	public StoredMessage remove(String identifier);

	public StoredMessage get(String identifier);
}
