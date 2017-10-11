package com.eastelsoft.etos2.mqtt.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.springframework.beans.factory.annotation.Autowired;

import eet.evar.core.redis.Redis;

public class RetainMessageStoreRedis implements RetainMessageStore {
	@Autowired
	private Redis redis;
	private Set<Topic> retainTopics = Collections.newSetFromMap(new ConcurrentHashMap<Topic,Boolean>());

	public void put(Topic topic, StoredMessage retainMessage) {
		redis.mapSet(RedisKeyBuilder.buildRetainMsgsKey(topic.toString()),
				topic.toString(), retainMessage);
		retainTopics.add(topic);
	}

	public StoredMessage remove(Topic topic) {
		if (topic == null) {
			return null;
		}
		retainTopics.remove(topic);
		StoredMessage retainMessage = redis.mapGetByHashKey(
				RedisKeyBuilder.buildRetainMsgsKey(topic.toString()),
				topic.toString(), StoredMessage.class);
		if (retainMessage != null) {
			redis.mapRemoveByHashKey(
					RedisKeyBuilder.buildRetainMsgsKey(topic.toString()),
					topic.toString());
		}
		return retainMessage;
	}

	public StoredMessage get(Topic topic) {
		if (topic == null) {
			return null;
		}
		return redis.mapGetByHashKey(
				RedisKeyBuilder.buildRetainMsgsKey(topic.toString()),
				topic.toString(), StoredMessage.class);
	}

	@Override
	public List<StoredMessage> match(Topic topic) {
		// TODO Auto-generated method stub
		List<StoredMessage> messages = new ArrayList<>();
		for(Topic retainTopic : retainTopics) {
			if (retainTopic.match(topic)) {
				StoredMessage message = get(retainTopic);
				if (message != null) {
					messages.add(message);
				}
			}
		}
		return messages;
	}

	public Redis getRedis() {
		return redis;
	}

	public void setRedis(Redis redis) {
		this.redis = redis;
	}

}
