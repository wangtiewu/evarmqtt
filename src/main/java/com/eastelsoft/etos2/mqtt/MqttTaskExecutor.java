package com.eastelsoft.etos2.mqtt;

public interface MqttTaskExecutor {
	public void submit(Runnable mqttTask);
	public void init();
	public void stop();
}
