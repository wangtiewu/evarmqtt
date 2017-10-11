# evarmatt

evarrpc is a java mqtt broker.It's base on netty.

## Features

* NetworkFramework use netty4.

* Hight performance.

* Cluster surport:pub/sub with redis or raft with atomix.

## Requirements

* Redis

## Usage 

* redis.properties, configure redis connection

* server_mqtt.properties, configure mqtt port or other parameters

* run on
```Java
eet.evar.tool.Logger.instance();
EvarBeanFactory.instance(new String[] {
				"spring-bean-container-cache.xml",
				"spring-bean-container-datasource.xml",
				"spring-bean-container-mqtt.xml" });
RedisBatchProcUtil.setRedis((Redis) EvarBeanFactory.instance().makeBean("redis"));
RedisBatchProcUtil.start();
String mqttConfigureFile = null;
MqttServer mqttServer = new NettyMqttServer(mqttConfigureFile);
mqttServer.start();
SubscriptionStore subscriptionStore = (SubscriptionStore) EvarBeanFactory.instance().makeBean(
				"subscriptionStore");
subscriptionStore.setMqttServer(mqttServer);
MqttNotifier mqttNotifier = (MqttNotifier) EvarBeanFactory.instance().makeBean("mqttNotifierTarget");
mqttNotifier.setMqttServer(mqttServer);
```