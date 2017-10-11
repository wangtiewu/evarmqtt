package com.eastelsoft.etos2.mqtt.server;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.eastelsoft.etos2.mqtt.server.TopicDirectoryFactory.TopicDirectoryType;

import eet.evar.tool.ConsistentHash;
import eet.evar.tool.HashAlgorithm;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class TopicDirectoryTrieHash implements TopicDirectory {
	private static final Logger logger = LoggerFactory
			.getLogger(TopicDirectoryTrieHash.class);
	private HashAlgorithm hashAlgorithm = HashAlgorithm.KETAMA_HASH;
	private List<TopicDirectory> topicDirTires = new ArrayList<>();
	private ConsistentHash<TopicDirectory> consistentHash = null;
	private int topicDirectoryTrieCount = 6;

	public TopicDirectoryTrieHash() {
		this(null);
	}

	public TopicDirectoryTrieHash(final Set<String> words) {
		init(words);
	}

	public void init() {
		init(null);
	}

	public void init(Set<String> words) {
		for (int i = 0; i < topicDirectoryTrieCount; i++) {
			TopicDirectory tire = TopicDirectoryFactory.makeTopicDirectory(TopicDirectoryType.DOUBLE_ARY);
			topicDirTires.add(tire);
		}
		consistentHash = new ConsistentHash(hashAlgorithm, 1, topicDirTires);
		if (words == null) {
			return;
		}
		for (String word : words) {
			TopicDirectory tire = consistentHash.get(word);
			tire.add(Topic.asTopic(word));
		}
	}

	public void destroy() {
		for (int i = 0; i < topicDirTires.size(); i++) {
			TopicDirectory tire = topicDirTires.get(i);
			tire.destroy();
		}
	}

	@Override
	public void add(Topic topic) {
		// TODO Auto-generated method stub
		final String topicFilter = topic.getTopicFilter();
		if (topicFilter == null || topicFilter.length() < 1) {
			return;
		}
		TopicDirectory tire = consistentHash.get(topicFilter);
		// System.out.println(tire);
		tire.add(topic);
	}

	@Override
	public void remove(Topic topic) {
		// TODO Auto-generated method stub
		final String topicFilter = topic.getTopicFilter();
		if (topicFilter == null || topicFilter.length() < 1) {
			return;
		}
		TopicDirectory tire = consistentHash.get(topicFilter);
		tire.remove(topic);
	}

	@Override
	public Set<Topic> match(Topic topic) {
		// TODO Auto-generated method stub
		final String topicFilter = topic.getTopicFilter();
		if (topicFilter == null || topicFilter.length() < 1) {
			return Collections.EMPTY_SET;
		}
		TopicDirectory tire = consistentHash.get(topicFilter);
		return tire.match(topic);
	}

	public String dumpTree() {
		// TODO Auto-generated method stub
		return "";
	}

	public static void main(String[] args) {
		eet.evar.tool.Logger.instance();
		int count = 800000;
		if (args.length > 0) {
			count = Integer.parseInt(args[0]);
		}
		for (int k = 0; k < 100; k++) {
			TopicDirectoryTrieHash trie = new TopicDirectoryTrieHash();
			long t = System.currentTimeMillis();
			Set<String> topicFilters = new HashSet<>();
			for (int i = 0; i < count; i++) {
				trie.add(Topic.asTopic(("abc/" + i + "/test")));
			}
			System.out.println("insert qos: " + count
					/ Math.max(1, ((System.currentTimeMillis() - t) / 1000)));
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			t = System.currentTimeMillis();
			for (int i = 0; i < count; i++) {
				if (1 !=
						trie.match(Topic.asTopic(("abc/" + i + "/test")))
								.size()) {
					throw new RuntimeException("match fail");
				}
			}
			System.out.println("match qos: " + count
					/ Math.max(1, ((System.currentTimeMillis() - t) / 1000)));

			t = System.currentTimeMillis();
			for (int i = 0; i < count; i++) {
				// trie.remove(Topic.asTopic(("abc/" + i + "/test")));
			}
			System.out.println("remove qos: " + count
					/ Math.max(1, ((System.currentTimeMillis() - t) / 1000)));
			trie.destroy();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
