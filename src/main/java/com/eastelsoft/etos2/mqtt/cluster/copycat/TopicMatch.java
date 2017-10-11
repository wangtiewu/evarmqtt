package com.eastelsoft.etos2.mqtt.cluster.copycat;

import java.util.Set;

import com.eastelsoft.etos2.mqtt.server.Topic;

import io.atomix.copycat.Query;

public class TopicMatch implements Query<Set<String>> {
	protected Topic topic;
	
	public TopicMatch(Topic topic) {
		this.topic = topic;
	}
}
