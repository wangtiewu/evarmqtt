package com.eastelsoft.etos2.mqtt.server;

import java.io.Writer;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.eastelsoft.etos2.mqtt.MqttConsts.ClusterType;
import com.eastelsoft.etos2.mqtt.cluster.PubSubStore;
import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.Listener;
import com.eastelsoft.etos2.mqtt.server.Namespace;
import com.eastelsoft.etos2.mqtt.server.NamespacesHub;
import com.eastelsoft.etos2.mqtt.server.SocketConfig;
import com.eastelsoft.etos2.mqtt.util.CancelableScheduler;
import com.eastelsoft.etos2.mqtt.util.HashedWheelScheduler;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;

import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

/**
 * Fully thread-safe.
 * 
 */
public abstract class MqttServer {
	private static final Logger log = LoggerFactory.getLogger(MqttServer.class);
	public static final String NAMESPACE_MQTT = "/mqtt";
	protected final Configuration configuration;
	protected final NamespacesHub namespacesHub;
	protected final Namespace mainNamespace;
	protected final long serverStartTime = System.currentTimeMillis();// 服务器启动时间
	protected final String serverId;// 格式 ip:port
	protected final ClusterType clusterType;
	protected final PubSubStore pubSubStore;
	protected CancelableScheduler scheduler = new HashedWheelScheduler();
	protected final Map<String, Queue<Listener>> eventListeners = new ConcurrentHashMap<String, Queue<Listener>>();
	protected final List<Listener> globalEventListeners = new ArrayList<Listener>();
	protected SSLContext serverSSLContext;
	protected Redis redis = (Redis) EvarBeanFactory.instance().makeBean("redis");


	public MqttServer() {
		this(false, false);
	}

	public MqttServer(boolean addIntfTraceLogListener,
			boolean addIntfPerfListener) {
		this.configuration = readConfiguration();
		serverId = configuration.getServerId();
		clusterType = configuration.getClusterType();
		pubSubStore = configuration.getStoreFactory().pubSubStore();
		namespacesHub = new NamespacesHub(this, configuration);
		mainNamespace = addNamespace(Namespace.DEFAULT_NAME);
	}

	/**
	 * 最近统计周期的读入字节数
	 * 
	 * @return
	 */
	public long lastReadBytes() {
		return 0L;
	}

	/**
	 * 最近统计周期的写入字节数
	 * 
	 * @return
	 */
	public long lastWrittenBytes() {
		return 0L;
	}

	/**
	 * 最近统计周期读bytes/s
	 * 
	 * @return
	 */
	public long lastReadThroughput() {
		return 0L;
	}

	/**
	 * 最近统计周期写bytes/s
	 * 
	 * @return
	 */
	public long lastWriteThroughput() {
		return 0L;
	}

	public int currentConnections() {
		return 0;
	}

	protected abstract Configuration readConfiguration();

	/**
	 * Get all clients connected to default namespace
	 * 
	 * @return clients collection
	 */
	public Collection<Client> getAllClients() {
		return namespacesHub.get(Namespace.DEFAULT_NAME).getAllClients();
	}

	/**
	 * Get client by uuid from default namespace
	 * 
	 * @param uuid
	 * @return
	 */
	public Client getClient(String uuid) {
		return namespacesHub.get(Namespace.DEFAULT_NAME).getClient(uuid);
	}

	public long getServerStartTime() {
		return serverStartTime;
	}

	/**
	 * Get all namespaces
	 * 
	 * @return namespaces collection
	 */
	public Collection<Namespace> getAllNamespaces() {
		return namespacesHub.getAllNamespaces();
	}

	public NamespacesHub getNamespacesHub() {
		return namespacesHub;
	}

	/**
	 * Start server
	 */
	public abstract boolean start();

	/**
	 * Stop server
	 */
	public void stop() {
		if (scheduler != null) {
			scheduler.shutdown();
		}
	}

	public Namespace addNamespace(String name) {
		Namespace namespace = namespacesHub.create(name);
		Queue<Listener> namespaceListeners = eventListeners.get(name);
		if (globalEventListeners.size() > 0) {
			if (namespaceListeners == null) {
				namespaceListeners = new ConcurrentLinkedQueue();
			}
			namespaceListeners.addAll(globalEventListeners);
		}
		if (namespaceListeners != null) {
			namespace.setListeners(namespaceListeners);
		}
		return namespace;
	}

	public Namespace addNamespace(String name, int ipAccLimCount) {
		Namespace namespace = namespacesHub.create(name, ipAccLimCount);
		Queue<Listener> namespaceListeners = eventListeners.get(name);
		if (globalEventListeners.size() > 0) {
			if (namespaceListeners == null) {
				namespaceListeners = new ConcurrentLinkedQueue();
			}
			namespaceListeners.addAll(globalEventListeners);
		}
		if (namespaceListeners != null) {
			namespace.setListeners(namespaceListeners);
		}
		return namespace;
	}

	public Namespace getNamespace(String name) {
		return namespacesHub.get(name);
	}

	public void removeNamespace(String name) {
		namespacesHub.remove(name);
	}

	/**
	 * Allows to get configuration provided during server creation. Further
	 * changes on this object not affect server.
	 * 
	 * @return Configuration object
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * 获取服务器ID，格式 ip:port
	 * 
	 * @return
	 */
	public String getServerId() {
		return serverId;
	}

	/**
	 * 获取调度器
	 * 
	 * @return
	 */
	public CancelableScheduler getScheduler() {
		return scheduler;
	}

	/**
	 * 增加监听器
	 */
	public void addListener(String event, Listener listener) {
		// TODO
		Queue<Listener> listeners = eventListeners.get(event);
		if (listeners == null) {
			listeners = new ConcurrentLinkedQueue();
			eventListeners.put(event, listeners);
		}
		listeners.add(listener);
	}

	/**
	 * 增加全局监听器
	 * 
	 * @param listener
	 */
	public void addGlobalListener(Listener listener) {
		globalEventListeners.add(listener);
	}

	public ClusterType getClusterType() {
		return clusterType;
	}

	public PubSubStore getPubSubStore() {
		return pubSubStore;
	}

}
