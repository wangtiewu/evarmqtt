package com.eastelsoft.etos2.mqtt.cluster.copycat;

import java.util.Map;

import com.eastelsoft.etos2.mqtt.server.Topic;

import io.atomix.copycat.Command;

public class TopicsUnsub implements Command<Void>{
	protected Map<Topic, String> subs;
	
	public TopicsUnsub(Map<Topic, String> subs) {
		this.subs = subs;
	}
}
