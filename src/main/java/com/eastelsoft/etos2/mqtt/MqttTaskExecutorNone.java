package com.eastelsoft.etos2.mqtt;

import org.springframework.beans.factory.annotation.Autowired;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttTaskExecutorNone implements MqttTaskExecutor {
	private final static Logger logger = LoggerFactory
			.getLogger(MqttTaskExecutorNone.class);
	private int workerCount = 0;

	public MqttTaskExecutorNone() {
	}

	@Override
	public void submit(Runnable mqttTask) {
		// TODO Auto-generated method stub
		mqttTask.run();
	}

	@Override
	public void init() {
	}

	@Override
	public void stop() {
	}

	public int getWorkerCount() {
		return workerCount;
	}

	public void setWorkerCount(int workerCount) {
		this.workerCount = workerCount;
	}
	
}
