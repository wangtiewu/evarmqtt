package com.eastelsoft.etos2.mqtt.server;

import java.util.List;


public interface RetainMessageStore {
	public void put(Topic topic, StoredMessage retainMessage);

	public StoredMessage remove(Topic topic);

	public StoredMessage get(Topic topic);
	
	public List<StoredMessage> match(Topic topic);
}
