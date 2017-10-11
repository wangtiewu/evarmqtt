package com.eastelsoft.etos2.mqtt.server;

import java.io.InputStream;
import java.util.Properties;

import com.eastelsoft.etos2.mqtt.MqttConsts.ClusterType;
import com.eastelsoft.etos2.mqtt.cluster.StoreFactory;
import com.eastelsoft.etos2.mqtt.cluster.memory.MemoryStoreFactory;
import com.eastelsoft.etos2.mqtt.util.JacksonJsonSupport;
import com.eastelsoft.etos2.mqtt.util.JsonSupport;
import com.eastelsoft.etos2.mqtt.util.XStreamXmlSupport;
import com.eastelsoft.etos2.mqtt.util.XmlSupport;

public class Configuration {
	private String context = "/socket.io";
	private int bossThreads = 0; // 0 = current_processors_amount * 2
	private int workerThreads = 0; // 0 = current_processors_amount * 2
	private boolean useLinuxNativeEpoll;
	private boolean allowCustomRequests = false;
	private int upgradeTimeout = 10000;
	private int pingTimeout = 60000;
	private int pingInterval = 25000;
	private int maxHttpContentLength = 64 * 1024;
	private int maxFramePayloadLength = 64 * 1024;
	private String packagePrefix;
	private String serverId;// 服务器id
	private String hostname;
	private int port = -1;
	private String sslProtocol = "TLSv1";
	private String keyStoreFormat = "JKS";
	private InputStream keyStore;
	private String keyStorePassword;
	private String trustStoreFormat = "JKS";
	private InputStream trustStore;
	private String trustStorePassword;
	private boolean preferDirectBuffer = true;
	private SocketConfig socketConfig = new SocketConfig();
	private boolean addVersionHeader = true;
	private String origin;
	private StoreFactory storeFactory = new MemoryStoreFactory();
	private JsonSupport jsonSupport = new JacksonJsonSupport();
	private XmlSupport xmlSupport = XStreamXmlSupport.getInstance();
	private boolean trafficShaping = true;
	/** mqtt protocol config begin **/
	private boolean zeroByteClientId = false;
	private boolean allowAnonymous = false;
	private ClusterType clusterType = ClusterType.NONE;
	/** mqtt protocol config end **/
	private Properties extProperties;

	public Configuration() {
	}

	/**
	 * Defend from further modifications by cloning
	 *
	 * @param configuration
	 *            - Configuration object to clone
	 */
	Configuration(Configuration conf) {
		setBossThreads(conf.getBossThreads());
		setWorkerThreads(conf.getWorkerThreads());
		setUseLinuxNativeEpoll(conf.isUseLinuxNativeEpoll());

		setPingInterval(conf.getPingInterval());

		setPingTimeout(conf.getPingTimeout());

		setHostname(conf.getHostname());
		setPort(conf.getPort());

		setContext(conf.getContext());
		setAllowCustomRequests(conf.isAllowCustomRequests());

		setKeyStorePassword(conf.getKeyStorePassword());
		setKeyStore(conf.getKeyStore());
		setKeyStoreFormat(conf.getKeyStoreFormat());
		setTrustStore(conf.getTrustStore());
		setTrustStoreFormat(conf.getTrustStoreFormat());
		setTrustStorePassword(conf.getTrustStorePassword());

		setMaxHttpContentLength(conf.getMaxHttpContentLength());
		setPackagePrefix(conf.getPackagePrefix());

		setPreferDirectBuffer(conf.isPreferDirectBuffer());
		setSocketConfig(conf.getSocketConfig());
		setMaxFramePayloadLength(conf.getMaxFramePayloadLength());
		setUpgradeTimeout(conf.getUpgradeTimeout());

		setAddVersionHeader(conf.isAddVersionHeader());
		setOrigin(conf.getOrigin());
		setSSLProtocol(conf.getSSLProtocol());
		setServerId(conf.getServerId());
		setStoreFactory(conf.getStoreFactory());
		setExtProperties(conf.getExtProperties());
		setClusterType(conf.getClusterType());
		setAllowAnonymous(conf.isAllowAnonymous());
		setZeroByteClientId(conf.isZeroByteClientId());
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public String getHostname() {
		return hostname;
	}

	/**
	 * Optional parameter. If not set then bind address will be 0.0.0.0 or ::0
	 *
	 * @param hostname
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getBossThreads() {
		return bossThreads;
	}

	public void setBossThreads(int bossThreads) {
		this.bossThreads = bossThreads;
	}

	public int getWorkerThreads() {
		return workerThreads;
	}

	public void setWorkerThreads(int workerThreads) {
		this.workerThreads = workerThreads;
	}

	/**
	 * Ping interval
	 *
	 * @param value
	 *            - time in milliseconds
	 */
	public void setPingInterval(int heartbeatIntervalSecs) {
		this.pingInterval = heartbeatIntervalSecs;
	}

	public int getPingInterval() {
		return pingInterval;
	}

	/**
	 * Ping timeout Use <code>0</code> to disable it
	 *
	 * @param value
	 *            - time in milliseconds
	 */
	public void setPingTimeout(int heartbeatTimeoutSecs) {
		this.pingTimeout = heartbeatTimeoutSecs;
	}

	public int getPingTimeout() {
		return pingTimeout;
	}

	public boolean isHeartbeatsEnabled() {
		return pingTimeout > 0;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public boolean isAllowCustomRequests() {
		return allowCustomRequests;
	}

	/**
	 * Allow to service custom requests differs from socket.io protocol. In this
	 * case it's necessary to add own handler which handle them to avoid hang
	 * connections. Default is {@code false}
	 *
	 * @param allowCustomRequests
	 *            - {@code true} to allow
	 */
	public void setAllowCustomRequests(boolean allowCustomRequests) {
		this.allowCustomRequests = allowCustomRequests;
	}

	/**
	 * SSL key store password
	 *
	 * @param keyStorePassword
	 */
	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	/**
	 * SSL key store stream, maybe appointed to any source
	 *
	 * @param keyStore
	 */
	public void setKeyStore(InputStream keyStore) {
		this.keyStore = keyStore;
	}

	public InputStream getKeyStore() {
		return keyStore;
	}

	/**
	 * Key store format
	 *
	 * @param keyStoreFormat
	 */
	public void setKeyStoreFormat(String keyStoreFormat) {
		this.keyStoreFormat = keyStoreFormat;
	}

	public String getKeyStoreFormat() {
		return keyStoreFormat;
	}

	/**
	 * Set maximum http content length limit
	 *
	 * @param maxContentLength
	 *            the maximum length of the aggregated http content.
	 */
	public void setMaxHttpContentLength(int value) {
		this.maxHttpContentLength = value;
	}

	public int getMaxHttpContentLength() {
		return maxHttpContentLength;
	}

	/**
	 * Package prefix for sending json-object from client without full class
	 * name.
	 *
	 * With defined package prefix socket.io client just need to define '@class:
	 * 'SomeType'' in json object instead of '@class:
	 * 'com.full.package.name.SomeType''
	 *
	 * @param packagePrefix
	 *            - prefix string
	 *
	 */
	public void setPackagePrefix(String packagePrefix) {
		this.packagePrefix = packagePrefix;
	}

	public String getPackagePrefix() {
		return packagePrefix;
	}

	/**
	 * Buffer allocation method used during packet encoding. Default is
	 * {@code true}
	 *
	 * @param preferDirectBuffer
	 *            {@code true} if a direct buffer should be tried to be used as
	 *            target for the encoded messages. If {@code false} is used it
	 *            will allocate a heap buffer, which is backed by an byte array.
	 */
	public void setPreferDirectBuffer(boolean preferDirectBuffer) {
		this.preferDirectBuffer = preferDirectBuffer;
	}

	public boolean isPreferDirectBuffer() {
		return preferDirectBuffer;
	}

	public SocketConfig getSocketConfig() {
		return socketConfig;
	}

	/**
	 * TCP socket configuration
	 *
	 * @param socketConfig
	 */
	public void setSocketConfig(SocketConfig socketConfig) {
		this.socketConfig = socketConfig;
	}

	public String getTrustStoreFormat() {
		return trustStoreFormat;
	}

	public void setTrustStoreFormat(String trustStoreFormat) {
		this.trustStoreFormat = trustStoreFormat;
	}

	public InputStream getTrustStore() {
		return trustStore;
	}

	public void setTrustStore(InputStream trustStore) {
		this.trustStore = trustStore;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	/**
	 * Set maximum websocket frame content length limit
	 *
	 * @param maxContentLength
	 */
	public void setMaxFramePayloadLength(int maxFramePayloadLength) {
		this.maxFramePayloadLength = maxFramePayloadLength;
	}

	public int getMaxFramePayloadLength() {
		return maxFramePayloadLength;
	}

	/**
	 * Transport upgrade timeout in milliseconds
	 *
	 * @param upgradeTimeout
	 */
	public void setUpgradeTimeout(int upgradeTimeout) {
		this.upgradeTimeout = upgradeTimeout;
	}

	public int getUpgradeTimeout() {
		return upgradeTimeout;
	}

	/**
	 * Adds <b>Server</b> header with lib version to http response. Default is
	 * <code>true</code>
	 *
	 * @param addVersionHeader
	 */
	public void setAddVersionHeader(boolean addVersionHeader) {
		this.addVersionHeader = addVersionHeader;
	}

	public boolean isAddVersionHeader() {
		return addVersionHeader;
	}

	/**
	 * Set <b>Access-Control-Allow-Origin</b> header value for http each
	 * response. Default is <code>null</code>
	 *
	 * If value is <code>null</code> then request <b>ORIGIN</b> header value
	 * used.
	 *
	 * @param origin
	 */
	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getOrigin() {
		return origin;
	}

	public boolean isUseLinuxNativeEpoll() {
		return useLinuxNativeEpoll;
	}

	public void setUseLinuxNativeEpoll(boolean useLinuxNativeEpoll) {
		this.useLinuxNativeEpoll = useLinuxNativeEpoll;
	}

	/**
	 * Set the name of the requested SSL protocol
	 *
	 * @param sslProtocol
	 */
	public void setSSLProtocol(String sslProtocol) {
		this.sslProtocol = sslProtocol;
	}

	public String getSSLProtocol() {
		return sslProtocol;
	}

	public StoreFactory getStoreFactory() {
		return storeFactory;
	}

	public void setStoreFactory(StoreFactory storeFactory) {
		this.storeFactory = storeFactory;
	}

	public JsonSupport getJsonSupport() {
		return jsonSupport;
	}

	public void setJsonSupport(JsonSupport jsonSupport) {
		this.jsonSupport = jsonSupport;
	}

	public boolean isTrafficShaping() {
		return trafficShaping;
	}

	public void setTrafficShaping(boolean trafficShaping) {
		this.trafficShaping = trafficShaping;
	}

	public Properties getExtProperties() {
		return extProperties;
	}

	public void setExtProperties(Properties extProperties) {
		this.extProperties = extProperties;
	}

	public String getFromExtProperties(String key) {
		return extProperties == null ? null : extProperties.getProperty(key);
	}

	public XmlSupport getXmlSupport() {
		// TODO Auto-generated method stub
		return this.xmlSupport;
	}

	public void setXmlSupport(XmlSupport xmlSupport) {
		this.xmlSupport = xmlSupport;
	}


	public ClusterType getClusterType() {
		return clusterType;
	}

	public void setClusterType(ClusterType clusterType) {
		this.clusterType = clusterType;
	}

	public boolean isZeroByteClientId() {
		return zeroByteClientId;
	}

	public void setZeroByteClientId(boolean zeroByteClientId) {
		this.zeroByteClientId = zeroByteClientId;
	}

	public boolean isAllowAnonymous() {
		return allowAnonymous;
	}

	public void setAllowAnonymous(boolean allowAnonymous) {
		this.allowAnonymous = allowAnonymous;
	}
	

}
