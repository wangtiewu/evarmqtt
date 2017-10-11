package com.eastelsoft.etos2.mqtt.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WillMessageStoreMemory implements WillMessageStore {
	private final Map<String, StoredMessage> willMsgMap = new ConcurrentHashMap<String, StoredMessage>();
	
	public void put(String identifier, StoredMessage willMessage) {
		willMsgMap.put(identifier, willMessage);
	}

	public StoredMessage remove(String identifier) {
		if (identifier == null) {
			return null;
		}
		return willMsgMap.remove(identifier);
	}

	public StoredMessage get(String identifier) {
		if (identifier == null) {
			return null;
		}
		return willMsgMap.get(identifier);
	}
}
