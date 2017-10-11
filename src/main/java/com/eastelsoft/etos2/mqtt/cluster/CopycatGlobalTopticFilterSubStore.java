package com.eastelsoft.etos2.mqtt.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eastelsoft.etos2.mqtt.MqttConsts.ClusterType;
import com.eastelsoft.etos2.mqtt.cluster.copycat.TopicSubStateMatchine;
import com.eastelsoft.etos2.mqtt.cluster.copycat.TopicSubStoreClient;
import com.eastelsoft.etos2.mqtt.cluster.copycat.TopicSubStoreNode;
import com.eastelsoft.etos2.mqtt.server.GlobalTopticFilterSubStore;
import com.eastelsoft.etos2.mqtt.server.Topic;

import eet.evar.tool.AbsBatchProcess;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class CopycatGlobalTopticFilterSubStore implements
		GlobalTopticFilterSubStore {
	private static final Logger logger = LoggerFactory
			.getLogger(CopycatGlobalTopticFilterSubStore.class);
	private TopicSubStoreNode topicSubStoreNode;
	private TopicSubStoreClient client;
	private String[] nodes;
	private String clusterType = ClusterType.NONE.name();
	private SubBatchProcess subBatchProcess;
	private UnsubBatchProcess unsubBatchProcess;
	private boolean batchProcessFlag = true;

	public void init() {
		if (!clusterType.equals(ClusterType.SUB_CLUSTER.name())) {
			logger.info("CopycatGlobalTopticFilterSubStore will not start, because clusterType is "
					+ clusterType);
			return;
		}
		if (nodes == null || nodes.length < 1) {
			logger.error("CopycatGlobalTopticFilterSubStore will not start, because nodes is not seted");
			return;
		}
		topicSubStoreNode = new TopicSubStoreNode(nodes);
		topicSubStoreNode.start();
		client = new TopicSubStoreClient(nodes);
		client.connect();
		subBatchProcess = new SubBatchProcess(100, 1000);
		subBatchProcess.start();
		unsubBatchProcess = new UnsubBatchProcess(100, 1000);
		unsubBatchProcess.start();
		logger.info("CopycatGlobalTopticFilterSubStore inited: nodes={}, batchProcessFlag={}", nodes, batchProcessFlag);
	}

	public void destroy() {
		if (subBatchProcess != null) {
			subBatchProcess.stop();
			subBatchProcess = null;
		}
		if (unsubBatchProcess != null) {
			unsubBatchProcess.stop();
			unsubBatchProcess = null;
		}
		if (client != null) {
			client.close();
			client = null;
		}
		if (topicSubStoreNode != null) {
			topicSubStoreNode.shutdown();
			topicSubStoreNode = null;
		}
	}

	@Override
	public void sub(Topic topicFilter, String serverId) {
		// TODO Auto-generated method stub
		if (batchProcessFlag) {
			subBatchProcess.batch(new Topic2ServerId(topicFilter, serverId));
		} else {
			client.sub(topicFilter, serverId);
		}
	}

	@Override
	public void unsub(Topic topicFilter, String serverId) {
		// TODO Auto-generated method stub
		if (batchProcessFlag) {
			unsubBatchProcess.batch(new Topic2ServerId(topicFilter, serverId));
		} else {
			client.unsub(topicFilter, serverId);
		}
	}

	@Override
	public Set<String> match(Topic topic) {
		// TODO Auto-generated method stub
		return TopicSubStateMatchine.match(topic);
//		return client.match(topic);
	}

	public String[] getNodes() {
		return nodes;
	}

	public void setNodes(String[] nodes) {
		this.nodes = nodes;
	}

	public String getClusterType() {
		return clusterType;
	}

	public void setClusterType(String clusterType) {
		this.clusterType = clusterType;
	}

	class Topic2ServerId {
		private Topic topicFilter;
		private String serverId;

		public Topic2ServerId(Topic topicFilter, String serverId) {
			this.topicFilter = topicFilter;
			this.serverId = serverId;
		}
	}

	class SubBatchProcess extends AbsBatchProcess<Topic2ServerId> {
		public SubBatchProcess(int batchSize, int maxWaitTime) {
			super(batchSize, maxWaitTime, 5 * batchSize);
		}

		@Override
		protected void process(List<Topic2ServerId> arg0) {
			// TODO Auto-generated method stub
			if (arg0.isEmpty()) {
				return;
			}
			Map<Topic, String> subs = new HashMap<>(arg0.size());
			arg0.stream().forEach(action -> {
				subs.put(action.topicFilter, action.serverId);
			});
			client.subs(subs);
		}
	}

	class UnsubBatchProcess extends AbsBatchProcess<Topic2ServerId> {
		public UnsubBatchProcess(int batchSize, int maxWaitTime) {
			super(batchSize, maxWaitTime, 5 * batchSize);
		}

		@Override
		protected void process(List<Topic2ServerId> arg0) {
			// TODO Auto-generated method stub
			if (arg0.isEmpty()) {
				return;
			}
			Map<Topic, String> subs = new HashMap<>(arg0.size());
			arg0.stream().forEach(action -> {
				subs.put(action.topicFilter, action.serverId);
			});
			client.unsubs(subs);
		}
	}

}
