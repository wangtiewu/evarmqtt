package com.eastelsoft.etos2.mqtt.cluster.copycat;

import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.eastelsoft.etos2.mqtt.server.Topic;
import com.eastelsoft.etos2.mqtt.server.TopicDirectory;
import com.eastelsoft.etos2.mqtt.server.TopicDirectoryTrieHash;

public class TopicSubStateMatchine extends StateMachine implements
		Snapshottable {
	private static Map<Topic, Set<String>> map = new ConcurrentHashMap<>();
	private static TopicDirectory topicDirectory = new TopicDirectoryTrieHash();

	public void subs(Commit<TopicsSub> commit) {
		try {
			TopicsSub topicsSub = commit.operation();
			Map<Topic, String> subs = topicsSub.subs;
			for (Map.Entry<Topic, String> entry : subs.entrySet()) {
				doSub(entry.getKey(), entry.getValue());
			}
		} finally {
			commit.release();
		}
	}

	public void sub(Commit<TopicSub> commit) {
		try {
			doSub(commit.operation().topicFilter, commit.operation().serverId);
		} finally {
			commit.release();
		}
	}

	private void doSub(Topic topicFilter, String serverId) {
		Set<String> nodes = map.get(topicFilter);
		if (nodes == null) {
			nodes = new HashSet<String>();
			Set<String> oldNodes = map.putIfAbsent(topicFilter, nodes);
			if (oldNodes != null) {
				nodes = oldNodes;
			}
		}
		nodes.add(serverId);
		topicDirectory.add(topicFilter);
		System.out.println("sub ok: topicFilter=" + topicFilter + ", nodeId="
				+ serverId);
	}

	public void unsubs(Commit<TopicsUnsub> commit) {
		try {
			TopicsUnsub topicsUnsub = commit.operation();
			Map<Topic, String> subs = topicsUnsub.subs;
			for (Map.Entry<Topic, String> entry : subs.entrySet()) {
				doUnsub(entry.getKey(), entry.getValue());
			}
		} finally {
			commit.release();
		}
	}

	public void unsub(Commit<TopicUnsub> commit) {
		try {
			doUnsub(commit.operation().topicFilter, commit.operation().serverId);
		} finally {
			commit.release();
		}
	}

	private void doUnsub(Topic topicFilter, String serverId) {
		Set<String> nodes = map.get(topicFilter);
		if (nodes != null) {
			nodes.remove(serverId);
			if (nodes.isEmpty()) {
				map.remove(topicFilter, Collections.EMPTY_SET);
			}
		}
		topicDirectory.remove(topicFilter);
		System.out.println("unsub ok: topicFilter=" + topicFilter + ", nodeId="
				+ serverId);
	}
	
	public static Set<String> match(Topic topic) {
		Set<Topic> topicFilters = topicDirectory
				.match(topic);
		if (topicFilters.isEmpty()) {
			return Collections.EMPTY_SET;
		}
		Set<String> nodes = new HashSet();
		for (Topic topicFilter : topicFilters) {
			nodes.addAll(map.getOrDefault(topicFilter,
					Collections.EMPTY_SET));
		}
		return nodes;
	}

	public Set<String> match(Commit<TopicMatch> commit) {
		try {
			Set<Topic> topicFilters = topicDirectory
					.match(commit.operation().topic);
			if (topicFilters.isEmpty()) {
				return Collections.EMPTY_SET;
			}
			Set<String> nodes = new HashSet();
			for (Topic topicFilter : topicFilters) {
				nodes.addAll(map.getOrDefault(topicFilter,
						Collections.EMPTY_SET));
			}
			return nodes;
		} finally {
			commit.release();
		}
	}

	@Override
	public void install(SnapshotReader arg0) {
		// TODO Auto-generated method stub
		map = arg0.readObject();
	}

	@Override
	public void snapshot(SnapshotWriter arg0) {
		// TODO Auto-generated method stub
		arg0.writeObject(map);
	}

}
