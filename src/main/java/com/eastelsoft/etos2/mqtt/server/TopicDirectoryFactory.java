package com.eastelsoft.etos2.mqtt.server;

public class TopicDirectoryFactory {
	public enum TopicDirectoryType {
		HASH, DOUBLE_ARY, DARS
	}

	public static TopicDirectory makeTopicDirectory(TopicDirectoryType type) {
		switch (type) {
		case HASH:
			return new TopicDirectoryTrieHash();
		case DOUBLE_ARY:
			return new TopicDirectoryTrie1();
		case DARS:
			return new TopicDirectoryTrie();
		default:
			return new TopicDirectoryTrie1();
		}
	}
}
