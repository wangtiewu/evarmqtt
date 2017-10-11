package com.eastelsoft.etos2.mqtt;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;

import com.eastelsoft.etos2.mqtt.util.AbortPolicyWithReport;
import com.eastelsoft.etos2.mqtt.util.NamedThreadFactory;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttTaskExecutorJdk implements MqttTaskExecutor {
	private final static Logger logger = LoggerFactory
			.getLogger(MqttTaskExecutorJdk.class);
	private ThreadPoolExecutor threadPoolExecutor;
	private int workerCount = Runtime.getRuntime().availableProcessors() * 2;
	private int maxWaitTask = -1;

	public MqttTaskExecutorJdk() {
	}

	@Override
	public void init() {
		threadPoolExecutor = (ThreadPoolExecutor) getExecutor(workerCount,
				maxWaitTask);
		logger.info("jdk thread pool executor started : 线程池大小：" + workerCount);

	}

	@Override
	public void submit(Runnable mqttTask) {
		// TODO Auto-generated method stub
		threadPoolExecutor.execute(mqttTask);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		if (threadPoolExecutor != null) {
			threadPoolExecutor.shutdown();
			threadPoolExecutor = null;
		}
		logger.info("jdk thread pool executor stoped");
	}

	private Executor getExecutor(int threads, int queues) {
		String name = "RpcThreadPool";
		return new ThreadPoolExecutor(threads, threads, 0,
				TimeUnit.MILLISECONDS,
				queues == 0 ? new SynchronousQueue<Runnable>()
						: (queues < 0 ? new LinkedBlockingQueue<Runnable>()
								: new LinkedBlockingQueue<Runnable>(queues)),
				new NamedThreadFactory(name, true), new AbortPolicyWithReport(
						name));
	}

	public int getWorkerCount() {
		return workerCount;
	}

	public void setWorkerCount(int workerCount) {
		this.workerCount = workerCount;
	}

}
