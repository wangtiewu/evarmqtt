package com.eastelsoft.etos2.mqtt.cluster.copycat;

import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.eastelsoft.etos2.mqtt.server.TopicDirectoryTrie;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class TopicSubStoreNode {
	private static final Logger logger = LoggerFactory
			.getLogger(TopicSubStoreNode.class);
	private String[] nodes;
	CopycatServer server;

	public TopicSubStoreNode(String[] nodes) {
		List<String> tmpNodes = new ArrayList();
		for (String node : nodes) {
			tmpNodes.addAll(Arrays.asList(node.split(",")));
		}
		this.nodes = tmpNodes.toArray(new String[0]);
	}

	public void start() {
		if (server == null) {
			String[] mainParts = nodes[0].split(":");
			Address address = new Address(mainParts[0],
					Integer.valueOf(mainParts[1]));
			List<Address> members = new ArrayList<>();
			for (int i = 0; i < nodes.length; i++) {
				String[] parts = nodes[i].split(":");
				members.add(new Address(parts[0], Integer.valueOf(parts[1])));
			}
			CopycatServer.Builder builder = CopycatServer.builder(address);
			builder.withStateMachine(TopicSubStateMatchine::new);
			builder.withTransport(NettyTransport.builder().build());
			builder.withStorage(Storage
					.builder()
					.withDirectory(
							new File(mainParts[0] + "_" + mainParts[1]
									+ "_logs"))
					.withStorageLevel(StorageLevel.DISK).build());
			server = builder.build();
			server.serializer().register(TopicSub.class, 1);
			server.serializer().register(TopicUnsub.class, 2);
			server.serializer().register(TopicMatch.class, 3);
			server.serializer().register(TopicsSub.class, 4);
			server.serializer().register(TopicsUnsub.class, 5);
			server.bootstrap(members).join();
//			server.bootstrap();
			logger.info("Copycat TopicSubStoreNode started");
		}
	}

	public void shutdown() {
		if (server != null) {
			server.shutdown();
			server = null;
			logger.info("Copycat TopicSubStoreNode stoped");
		}
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			throw new IllegalArgumentException(
					"must supply set of host:port tuples");
		}
		eet.evar.tool.Logger.instance();
		TopicSubStoreNode node = new TopicSubStoreNode(args);
		node.start();
	}
}
