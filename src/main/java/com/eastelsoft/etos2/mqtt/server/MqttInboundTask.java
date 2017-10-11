package com.eastelsoft.etos2.mqtt.server;

import java.util.Map;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.Namespace;

public class MqttInboundTask implements Runnable {
	private final Namespace namespace;
	private final Client client;
	private final String eventName;
	private final Map parameters;
	private final Object arg;

	public MqttInboundTask(Namespace namespace, Client client,
			String eventName, Map parameters, Object arg) {
		this.namespace = namespace;
		this.client = client;
		this.eventName = eventName;
		this.parameters = parameters;
		this.arg = arg;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		namespace.onEvent(client, eventName, null, arg);
	}

}
