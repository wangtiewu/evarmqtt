package com.eastelsoft.etos2.mqtt.server;

import java.util.Set;

public interface GlobalTopticFilterSubStore {
	void sub(Topic topicFilter, String serverId);
	void unsub(Topic topicFilter, String serverId);
	Set<String> match(Topic topic);
}
