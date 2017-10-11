package com.eastelsoft.etos2.mqtt;

import org.springframework.beans.factory.annotation.Autowired;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.RoundRobinPool;

public class MqttTaskExecutorAkka implements MqttTaskExecutor {
	private final static Logger logger = LoggerFactory
			.getLogger(MqttTaskExecutorAkka.class);
	private ActorSystem system = null;
	private ActorRef actor = null;
	private int workerCount = Runtime.getRuntime().availableProcessors() * 2;

	public MqttTaskExecutorAkka() {
	}
	
	public void init() {
		system = ActorSystem.create("MqttTaskExecutorAkka");
		actor = system
				.actorOf(
						Props.create(HandleAkkaActor.class, this).withRouter(
								new RoundRobinPool(workerCount)),
						"handleAkkaActor");
		logger.info("akka executor started : router 策略：RoundRobin，数量：" + workerCount);
	}

	@Override
	public void submit(Runnable mqttTask) {
		// TODO Auto-generated method stub
		actor.tell(mqttTask, null);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		if (system != null && actor != null) {
			system.stop(actor);
			actor = null;
		}
		system.shutdown();
		logger.info("akka executor stoped");
	}

	class HandleAkkaActor extends UntypedActor {

		@Override
		public void onReceive(Object arg0) throws Exception {
			// TODO Auto-generated method stub
			if (arg0 instanceof Runnable) {
				((Runnable) arg0).run();
			} else {
				unhandled(arg0);
			}
		}

	}

	public int getWorkerCount() {
		return workerCount;
	}

	public void setWorkerCount(int workerCount) {
		this.workerCount = workerCount;
	}
	
}
