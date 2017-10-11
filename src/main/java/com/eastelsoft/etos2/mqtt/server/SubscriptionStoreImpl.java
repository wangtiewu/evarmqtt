package com.eastelsoft.etos2.mqtt.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.eastelsoft.etos2.mqtt.MqttConsts.ClusterType;
import com.eastelsoft.etos2.mqtt.server.TopicDirectoryFactory.TopicDirectoryType;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class SubscriptionStoreImpl implements SubscriptionStore {
	private static final Logger logger = LoggerFactory
			.getLogger(SubscriptionStoreImpl.class);
	final private TopicDirectory topicDirectory = TopicDirectoryFactory.makeTopicDirectory(TopicDirectoryType.HASH);
	final private Map<Topic, Set<Subscription>> topic2SubMap = new ConcurrentHashMap<>();
	private MqttServer mqttServer;
	private GlobalTopticFilterSubStore globalTopticFilterSubStore;

	@Override
	public void sub(Subscription subscription) {
		// TODO Auto-generated method stub
		if (subscription == null) {
			return;
		}
		topicDirectory.add(subscription.getTopic());
		Set<Subscription> subs = topic2SubMap.get(subscription.getTopic());
		if (subs == null) {
			subs = new HashSet<Subscription>();
			Set<Subscription> oldSubs = topic2SubMap.putIfAbsent(
					subscription.getTopic(), subs);
			if (oldSubs != null) {
				subs = oldSubs;
			}
			if (mqttServer.getClusterType() == ClusterType.SUB_CLUSTER) {
				globalTopticFilterSubStore.sub(subscription.getTopic(),
						mqttServer.getServerId());
			}
		}
		subs.add(subscription);
		logger.info("Subscription success stored： {}", subscription);
	}

	@Override
	public void sub(Set<Subscription> subscriptions) {
		if (subscriptions == null) {
			return;
		}
		for (Subscription sub : subscriptions) {
			sub(sub);
		}
	}

	@Override
	public void unsub(Subscription subscription) {
		// TODO Auto-generated method stub
		if (subscription == null) {
			return;
		}
		topicDirectory.remove(subscription.getTopic());
		Set<Subscription> subs = topic2SubMap.get(subscription.getTopic());
		if (subs != null) {
			if (subs.remove(subscription)) {
				if (subs.isEmpty()) {
					topic2SubMap.remove(subscription.getTopic(),
							Collections.emptySet());
					if (mqttServer.getClusterType() == ClusterType.SUB_CLUSTER) {
						globalTopticFilterSubStore.unsub(
								subscription.getTopic(),
								mqttServer.getServerId());
					}
				}
				logger.info("Topic'subscription success removed： {}",
						subscription);
			}
		}
	}

	@Override
	public void unsub(Set<Subscription> subscriptions) {
		if (subscriptions == null) {
			return;
		}
		for (Subscription sub : subscriptions) {
			unsub(sub);
		}
	}

	@Override
	public List<Subscription> match(Topic topic) {
		// TODO Auto-generated method stub
		if (topic == null) {
			return Collections.EMPTY_LIST;
		}
		Set<Topic> matchedTopics = topicDirectory.match(topic);
		List<Subscription> subs = new ArrayList<Subscription>(
				matchedTopics.size());
		for (Topic matchedTopic : matchedTopics) {
			Set<Subscription> topicSubs = topic2SubMap.get(matchedTopic);
			if (topicSubs != null) {
				subs.addAll(topicSubs);
			}
		}
		return subs;
	}

	public MqttServer getMqttServer() {
		return mqttServer;
	}

	public void setMqttServer(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}

	public GlobalTopticFilterSubStore getGlobalTopticFilterSubStore() {
		return globalTopticFilterSubStore;
	}

	public void setGlobalTopticFilterSubStore(
			GlobalTopticFilterSubStore globalTopticFilterSubStore) {
		this.globalTopticFilterSubStore = globalTopticFilterSubStore;
	}

}
