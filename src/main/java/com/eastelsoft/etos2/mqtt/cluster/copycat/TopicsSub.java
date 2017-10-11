package com.eastelsoft.etos2.mqtt.cluster.copycat;

import java.util.Map;

import com.eastelsoft.etos2.mqtt.server.Topic;

import io.atomix.copycat.Command;

public class TopicsSub implements Command<Void>{
	protected Map<Topic, String> subs;
	
	public TopicsSub(Map<Topic, String> subs) {
		this.subs = subs;
	}
}
