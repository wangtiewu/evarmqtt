package com.eastelsoft.etos2.mqtt.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.eastelsoft.etos2.rpc.tool.StringDeal;

import eet.evar.tool.AbsBatchProcess;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;
import eet.evar.tool.trie.TrieUtil;
import eet.evar.tool.trie.mtext.SimpleDartsSegment;

public class TopicDirectoryTrie implements TopicDirectory {
	private static final Logger logger = LoggerFactory.getLogger(TopicDirectoryTrie.class);
	private static final int MAX_APPEND_WORDS_COUNT = 100000;
	private volatile SimpleDartsSegment mainDarts;
	private volatile SimpleDartsSegment appendDarts;
	private Set<String> words = Collections
			.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	private Set<String> addedWords = Collections
			.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	private BatchProcess batchProcess;
	private int maxWaitTime = 1000;
	private int batchSie = 10000;

	protected TopicDirectoryTrie() {
		this(null);
	}

	public TopicDirectoryTrie(final Set<String> words) {
		if (words != null) {
			this.words.addAll(words);
		}
		init();
	}

	public void init() {
		init(null);
	}

	public void init(Set<String> words) {
		if (words != null) {
			this.words.addAll(words);
		}
		mainDarts = TrieUtil.genDartsSegment(this.words);
		appendDarts = TrieUtil.genDartsSegment(new ArrayList<char[]>());
		if (batchProcess == null) {
			batchProcess = new BatchProcess(batchSie, maxWaitTime);
			batchProcess.start();
		}
	}

	public void destroy() {
		if (batchProcess != null) {
			batchProcess.stop();
			batchProcess = null;
		}
	}

	@Override
	public void add(Topic topic) {
		// TODO Auto-generated method stub
		final String topicFilter = topic.getTopicFilter();
		if (topicFilter == null || topicFilter.length() < 1) {
			return;
		}
		batchProcess.batch(new TopicOper(topic));
	}

	@Override
	public void remove(Topic topic) {
		// TODO Auto-generated method stub
		final String topicFilter = topic.getTopicFilter();
		if (topicFilter == null || topicFilter.length() < 1) {
			return;
		}
		batchProcess.batch(new TopicOper(topic, true));
	}

	@Override
	public Set<Topic> match(Topic topic) {
		// TODO Auto-generated method stub
		final String topicFilter = topic.getTopicFilter();
		if (topicFilter == null || topicFilter.length() < 1) {
			return Collections.EMPTY_SET;
		}
		Set<Topic> topics = match(mainDarts, topic);
		topics.addAll(match(appendDarts, topic));
		return topics;
	}

	public Set<Topic> match(SimpleDartsSegment darts, Topic topic) {
		// TODO Auto-generated method stub
		final String topicFilter = topic.getTopicFilter();
		if (topicFilter == null || topicFilter.length() < 1) {
			return Collections.EMPTY_SET;
		}
		Set<Topic> topics = new HashSet<>();
		Collection<char[]> tokens1 = TrieUtil.getToken(darts, topicFilter);
		addTopics(topics, tokens1, topicFilter);

		StringBuffer topicFilterTmp = new StringBuffer(topicFilter);
		if (topicFilter.endsWith(Topic.TOKEN_SPILIT)) {
			topicFilterTmp.append("#");
		} else {
			topicFilterTmp.append(Topic.TOKEN_SPILIT).append("#");
		}
		tokens1 = TrieUtil.getToken(darts, topicFilterTmp);
		addTopics(topics, tokens1, topicFilterTmp.toString());

		topicFilterTmp.setLength(0);
		topicFilterTmp.append(topicFilter);
		if (topicFilter.endsWith(Topic.TOKEN_SPILIT)) {
			topicFilterTmp.append("+");
		} else {
			topicFilterTmp.append(Topic.TOKEN_SPILIT).append("+");
		}
		tokens1 = TrieUtil.getToken(darts, topicFilterTmp);
		addTopics(topics, tokens1, topicFilterTmp.toString());

		String[] tokens = StringDeal.split(topicFilter, Topic.TOKEN_SPILIT);
		for (int i = 0; i < tokens.length; i++) {
			if (i == 0) {
				topicFilterTmp.setLength(0);
				topicFilterTmp.append("#");
				tokens1 = TrieUtil.getToken(darts, topicFilterTmp);
				addTopics(topics, tokens1, topicFilterTmp.toString());
				topicFilterTmp.setLength(0);
				topicFilterTmp.append("+");
				for (int j = 1; j < tokens.length; j++) {
					topicFilterTmp.append(Topic.TOKEN_SPILIT).append(tokens[j]);
				}
				tokens1 = TrieUtil.getToken(darts, topicFilterTmp);
				addTopics(topics, tokens1, topicFilterTmp.toString());
			} else {
				StringBuffer postTopicFilter = new StringBuffer("#");
				int preTokenSize = i;
				for (int j = 0; j < Math.pow(2, preTokenSize); j++) {
					byte[] bits = getBooleanArray(j, preTokenSize);
					topicFilterTmp.setLength(0);
					for (int g = 0; g < bits.length; g++) {
						if (bits[g] == 0) {
							topicFilterTmp.append("+").append(
									Topic.TOKEN_SPILIT);
						} else {
							topicFilterTmp.append(tokens[g]).append(
									Topic.TOKEN_SPILIT);
						}
					}
					topicFilterTmp.append(postTopicFilter);
					tokens1 = TrieUtil.getToken(darts, topicFilterTmp);
					addTopics(topics, tokens1, topicFilterTmp.toString());
				}

				postTopicFilter.setLength(0);
				postTopicFilter.append("+");
				for (int j = (i + 1); j < tokens.length; j++) {
					postTopicFilter.append(Topic.TOKEN_SPILIT)
							.append(tokens[j]);
				}
				preTokenSize = i;
				for (int j = 0; j < Math.pow(2, preTokenSize); j++) {
					byte[] bits = getBooleanArray(j, preTokenSize);
					topicFilterTmp.setLength(0);
					for (int g = 0; g < bits.length; g++) {
						if (bits[g] == 0) {
							topicFilterTmp.append("+").append(
									Topic.TOKEN_SPILIT);
						} else {
							topicFilterTmp.append(tokens[g]).append(
									Topic.TOKEN_SPILIT);
						}
					}
					topicFilterTmp.append(postTopicFilter);
					tokens1 = TrieUtil.getToken(darts, topicFilterTmp);
					addTopics(topics, tokens1, topicFilterTmp.toString());
				}
			}
		}
		return topics;
	}

	private void addTopics(Set<Topic> topics, Collection<char[]> topicFilters,
			String topicFilter1) {
		if (topicFilters != null && !topicFilters.isEmpty()) {
			for (char[] topicFilter : topicFilters) {
				if (topicFilter1.equals(new String(topicFilter))) {
					topics.add(Topic.asTopic(new String(topicFilter)));
					return;
				}
			}
		}
	}

	public String dumpTree() {
		// TODO Auto-generated method stub
		return "";
	}

	private static byte[] getBooleanArray(int b, int size) {
		byte[] array = new byte[size];
		for (int i = size - 1; i >= 0; i--) {
			array[i] = (byte) (b & 1);
			b = (byte) (b >> 1);
		}
		return array;
	}

	public static void main(String[] args) {
		eet.evar.tool.Logger.instance();
		int count = 800000;
		if (args.length > 0) {
			count = Integer.parseInt(args[0]);
		}
		for (int k = 0; k < 100; k++) {
			TopicDirectoryTrie trie = new TopicDirectoryTrie();
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

	class TopicOper {
		private Topic topic;
		private boolean del = false;

		public TopicOper(Topic topic) {
			this(topic, false);
		}

		public TopicOper(Topic topic, boolean del) {
			this.topic = topic;
			this.del = del;
		}
	}

	class BatchProcess extends AbsBatchProcess<TopicOper> {
		private int addOrRemovedCount = 0;

		public BatchProcess(int batchSize, int maxWaitTime) {
			super(batchSize, maxWaitTime, 5 * batchSize);
		}

		@Override
		protected void process(List<TopicOper> arg0) {
			// TODO Auto-generated method stub
			if (arg0.isEmpty()) {
				return;
			}
			arg0.stream().forEach(topicOpen -> {
				String topicFilter = topicOpen.topic.getTopicFilter();
				if (topicOpen.del) {
					if (!addedWords.remove(topicFilter)) {
						if (words.remove(topicFilter)) {
							addOrRemovedCount++;
						}
					} else {
						addOrRemovedCount++;
					}
				} else {
					addedWords.add(topicOpen.topic.getTopicFilter());
					addOrRemovedCount++;
				}
			});
			if (addOrRemovedCount >= MAX_APPEND_WORDS_COUNT) {
				words.addAll(addedWords);
				SimpleDartsSegment newMainDarts = TrieUtil
						.genDartsSegment(words);
				mainDarts = newMainDarts;
				SimpleDartsSegment newAppendDarts = TrieUtil.genDartsSegment(new ArrayList<char[]>());
				appendDarts = newAppendDarts;
				logger.info("变更topcis数量超过 {}，merger topics, rebuild mainDarts and reset appendDarts", addOrRemovedCount);
				addedWords.clear();
				addOrRemovedCount = 0;
			} else {
				SimpleDartsSegment newAppendDarts = TrieUtil
						.genDartsSegment(addedWords);
				appendDarts = newAppendDarts;
			}
		}

	}
}
