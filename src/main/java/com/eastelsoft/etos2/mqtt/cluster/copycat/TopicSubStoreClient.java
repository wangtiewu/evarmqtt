package com.eastelsoft.etos2.mqtt.cluster.copycat;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.ConnectionStrategies;
import io.atomix.copycat.client.ConnectionStrategy;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.client.RecoveryStrategies;
import io.atomix.copycat.client.ServerSelectionStrategies;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.eastelsoft.etos2.mqtt.server.Topic;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class TopicSubStoreClient {
	private static final Logger logger = LoggerFactory
			.getLogger(TopicSubStoreClient.class);

	private String[] servers;
	private CopycatClient client = null;

	public TopicSubStoreClient(String[] servers) {
		List<String> tmpNodes = new ArrayList();
		for (String node : servers) {
			tmpNodes.addAll(Arrays.asList(node.split(",")));
		}
		this.servers = tmpNodes.toArray(new String[0]);
	}

	public void connect() {
		List<Address> addresses = new ArrayList<Address>();
		for (String server : servers) {
			addresses.add(new Address(server.split(":")[0], Integer
					.parseInt(server.split(":")[1])));
		}
		client = CopycatClient.builder()
				.withTransport(NettyTransport.builder().build())
				.withConnectionStrategy(ConnectionStrategies.FIBONACCI_BACKOFF)
				.withRecoveryStrategy(RecoveryStrategies.RECOVER)
				.withServerSelectionStrategy(ServerSelectionStrategies.LEADER)
				.withSessionTimeout(Duration.ofSeconds(15)).build();
		client.serializer().register(TopicSub.class, 1);
		client.serializer().register(TopicUnsub.class, 2);
		client.serializer().register(TopicMatch.class, 3);
		client.serializer().register(TopicsSub.class, 4);
		client.serializer().register(TopicsUnsub.class, 5);
		client.connect(addresses).join();
		logger.info("Copycat TopicSubStoreClinet connected cluster {} ok",
				servers);
	}

	public void close() {
		if (client != null) {
			client.close().join();
			client = null;
			logger.info("Copycat TopicSubStoreClinet closed");
		}
	}

	public void subs(Map<Topic, String> subs) {
		client.submit(new TopicsSub(subs));
	}
	
	public void sub(Topic topicFilter, String serverId) {
		client.submit(new TopicSub(topicFilter, serverId));
		logger.info("sub topic message submit  copycat cluster: topicFilter: {}, serverId: {}", topicFilter, serverId);
	}

	public void unsubs(Map<Topic, String> subs) {
		client.submit(new TopicsUnsub(subs));
	}
	public void unsub(Topic topicFilter, String serverId) {
		client.submit(new TopicUnsub(topicFilter, serverId));
		logger.info("unsub topic message submit to copycat cluster: topicFilter: {}, serverId: {}", topicFilter, serverId);

	}

	public Set<String> match(Topic topic) {
		try {
			return client.submit(new TopicMatch(topic)).get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Collections.EMPTY_SET;
		}
	}

	public static void main(String[] args) {
//		String[] servers = new String[] { "10.0.65.104:5000",
//				"10.0.65.104:5001", "10.0.66.26:5000" };
//		String[] servers = new String[] { "10.0.66.26:5000",
//				"10.0.66.26:5001", "10.0.66.26:5000" };
		String[] servers = new String[] { "10.0.69.28:5000",
				"10.0.69.28:5001", "10.0.69.28:5002" };
		TopicSubStoreClient client = new TopicSubStoreClient(servers);
		client.connect();
		Topic topicFilter = Topic.asTopic("a/b/c");
		String serverId = "127.0.0.1:9000";
		client.sub(topicFilter, serverId);
		client.sub(Topic.asTopic("#"), "127.0.0.1:9001");
		client.sub(Topic.asTopic("a/b"), "127.0.0.1:9003");
		client.sub(Topic.asTopic("a"), "127.0.0.1:9004");
		Map<Topic, String> subs = new HashMap<>();
		subs.put(Topic.asTopic("#"), "127.0.0.1:9005");
		subs.put(Topic.asTopic("a"), "127.0.0.1:9004");
		client.subs(subs);
		// client.unsub(topicFilter, serverId);
		Set<String> nodes = client.match(Topic.asTopic("a/b"));
		System.out.println(nodes);
		
		client.unsubs(subs);
		nodes = client.match(Topic.asTopic("a/b"));
		System.out.println(nodes);
	}
}
