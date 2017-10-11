package com.eastelsoft.etos2.mqtt.cluster.copycat;

import com.eastelsoft.etos2.mqtt.server.Topic;

import io.atomix.copycat.Command;

public class TopicSub implements Command<Void>{
	protected Topic topicFilter;
	protected String serverId;
	
	public TopicSub(Topic topicFilter, String serverId) {
		this.topicFilter = topicFilter;
		this.serverId = serverId;
	}
}
