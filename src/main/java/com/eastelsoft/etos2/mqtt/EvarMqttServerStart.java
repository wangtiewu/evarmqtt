package com.eastelsoft.etos2.mqtt;

import com.eastelsoft.etos2.mqtt.cluster.MqttNotifier;
import com.eastelsoft.etos2.mqtt.server.MqttServer;
import com.eastelsoft.etos2.mqtt.server.SubscriptionStore;
import com.eastelsoft.etos2.mqtt.server.netty.NettyMqttServer;
import com.eastelsoft.etos2.mqtt.util.RedisBatchProcUtil;

import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class EvarMqttServerStart {
	private static Logger logger = LoggerFactory
			.getLogger(EvarMqttServerStart.class);
	public static volatile boolean running = true;

	public static void main(String[] args) {
		eet.evar.tool.Logger.instance();
		EvarBeanFactory.instance(new String[] {
				"spring-bean-container-cache.xml",
				"spring-bean-container-datasource.xml",
				"spring-bean-container-mqtt.xml" });
		RedisBatchProcUtil.setRedis((Redis) EvarBeanFactory.instance().makeBean("redis"));
		RedisBatchProcUtil.start();
		String mqttConfigureFile = null;
		if (args.length > 0) {
			mqttConfigureFile = args[0];
		}
		MqttServer mqttServer = new NettyMqttServer(mqttConfigureFile);
		mqttServer.start();
		SubscriptionStore subscriptionStore = (SubscriptionStore) EvarBeanFactory.instance().makeBean(
				"subscriptionStore");
		subscriptionStore.setMqttServer(mqttServer);
		MqttNotifier mqttNotifier = (MqttNotifier) EvarBeanFactory.instance().makeBean("mqttNotifierTarget");
		mqttNotifier.setMqttServer(mqttServer);
	}

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				logger.info("EvarMqttServerStart shutdown");
				running = false;
				RedisBatchProcUtil.stop();
				EvarBeanFactory.destroy();
			}
		}, "EvarMqttServerStart shutdown hook"));
	}
}
