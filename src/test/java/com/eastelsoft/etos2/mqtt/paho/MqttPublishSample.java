package com.eastelsoft.etos2.mqtt.paho;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


public class MqttPublishSample {
	private static void testPub(String topic, final int count, final int payloadSize) throws MqttException {
		final CountDownLatch finished = new CountDownLatch(count);
		StringBuffer payload = new StringBuffer();
		int size = payloadSize;
		while (size-- > 0) {
			payload.append(size % 2 == 0 ? "0" : "1");
		}
		final String str = payload.toString();
		int qos = 1;
		String broker = "tcp://10.0.65.105:1883";
		String clientId = "paho800899651999622";
		MemoryPersistence persistence = new MemoryPersistence();
		MqttClient sampleClient = new MqttClient(broker, clientId,
				persistence);
		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(true);
		System.out.println("Connecting to broker: " + broker);
		sampleClient.connect(connOpts);
		MqttMessage message = new MqttMessage(str.getBytes());
		message.setQos(qos);
		long t1 = System.currentTimeMillis();
		for(int i=0; i<count; i++) {
			sampleClient.publish(topic, message);
		}
		System.out.println(payloadSize
				+ " bytes test: 请求数 " + count * 1
				+ "，耗时 "+ (System.currentTimeMillis() - t1)
				+ "，qps "+ count * 1 / ((System.currentTimeMillis() - t1) / 1000 == 0 ? 1 : (System
						.currentTimeMillis() - t1) / 1000)+"，mbps "+ (2L * count * 1 * payloadSize)/1000/((System.currentTimeMillis() - t1) / 1000 == 0 ? 1 : (System
								.currentTimeMillis() - t1)));
		sampleClient.disconnect();
		System.out.println("Disconnected");
	}
	
	private static void testSub(String topic, final int count, final int payloadSize) throws MqttException {
		final CountDownLatch finished = new CountDownLatch(count);
		StringBuffer payload = new StringBuffer();
		int size = payloadSize;
		while (size-- > 0) {
			payload.append(size % 2 == 0 ? "0" : "1");
		}
		final String str = payload.toString();
		int qos = 1;
		String broker = "tcp://10.0.65.105:1883";
		String clientId = "paho800899651999621";
		MemoryPersistence persistence = new MemoryPersistence();
		MqttClient sampleClient = new MqttClient(broker, clientId,
				persistence);
		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(true);
		System.out.println("Connecting to broker: " + broker);
		sampleClient.connect(connOpts);
		long t1 = System.currentTimeMillis();
		sampleClient.setCallback(new MqttCallback() {
			AtomicInteger ac = new AtomicInteger();
			@Override
			public void messageArrived(String arg0, MqttMessage arg1)
					throws Exception {
				// TODO Auto-generated method stub
//				System.out.println("messageArrived: " + arg0 + ", " + arg1);
				finished.countDown();
				if (ac.incrementAndGet() % 1000 == 0) {
					System.out.println("messageArrived: " + arg0 + ", " + arg1);
				}
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void connectionLost(Throwable arg0) {
				// TODO Auto-generated method stub

			}
		});
		sampleClient.subscribe(topic, qos);
		try {
			finished.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(payloadSize
				+ " bytes test: 请求数 " + count * 1
				+ "，耗时 "+ (System.currentTimeMillis() - t1)
				+ "，qps "+ count * 1 / ((System.currentTimeMillis() - t1) / 1000 == 0 ? 1 : (System
						.currentTimeMillis() - t1) / 1000)+"，mbps "+ (2L * count * 1 * payloadSize)/1000/((System.currentTimeMillis() - t1) / 1000 == 0 ? 1 : (System
								.currentTimeMillis() - t1)));
		sampleClient.disconnect();
		System.out.println("Disconnected");
	}

	public static void main(String[] args) {
		final String topic = "#";
		final int count = 1*10000;
		Thread pubThread = new Thread() {
			public void run() {
				try {
					testPub(topic, count, 64);
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
//		pubThread.start();
		
		Thread subThread = new Thread() {
			public void run() {
				try {
					testSub(topic, count, 64);
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		subThread.start();
		
		try {
			pubThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			subThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
