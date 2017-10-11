package com.eastelsoft.etos2.mqtt.cluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.eastelsoft.etos2.mqtt.server.StoredMessage;
import com.eastelsoft.etos2.rpc.RpcClient;
import com.eastelsoft.etos2.rpc.serialize.protostuff.ProtostuffRpcRespSerialize;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttNotifierHelper {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttNotifierHelper.class);
	private static Map<String, MqttNotifier> publishers = new ConcurrentHashMap<>();

	public static void disconnect(String serverId, String sessionId) {
		MqttNotifier publisher = publishers.get(serverId);
		if (publisher == null) {
			synchronized (publishers) {
				if (!publishers.containsKey(serverId)) {
					try {
						publisher = RpcClient.ref(MqttNotifier.class, serverId,
								ProtostuffRpcRespSerialize.class);
						publishers.put(serverId, publisher);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						logger.error("通知节点 " + serverId + "关闭连接 " + sessionId
								+ " 失败：" + e.getMessage(), e);
						return;
					}
				}
			}
		}
		publisher.disconnect(sessionId);
	}

	public static void publish(String serverId, StoredMessage storedMessage) {
		MqttNotifier publisher = publishers.get(serverId);
		if (publisher == null) {
			synchronized (publishers) {
				if (!publishers.containsKey(serverId)) {
					try {
						publisher = RpcClient.ref(MqttNotifier.class, serverId,
								ProtostuffRpcRespSerialize.class);
						publishers.put(serverId, publisher);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						logger.error(
								"通知节点 " + serverId + " delivery "
										+ storedMessage.toString() + " 失败："
										+ e.getMessage(), e);
						return;
					}
				}
			}
		}
		publisher.publish(storedMessage);
	}

	public static void publish(String serverId, StoredMessage storedMessage,
			String... identifiers) {
		MqttNotifier publisher = publishers.get(serverId);
		if (publisher == null) {
			synchronized (publishers) {
				if (!publishers.containsKey(serverId)) {
					try {
						publisher = RpcClient.ref(MqttNotifier.class, serverId,
								ProtostuffRpcRespSerialize.class);
						publishers.put(serverId, publisher);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						logger.error(
								"通知节点 " + serverId + " delivery "
										+ storedMessage.toString() + " 失败："
										+ e.getMessage(), e);
						return;
					}
				}
			}
		}
		publisher.publish(storedMessage, identifiers);
	}
}
