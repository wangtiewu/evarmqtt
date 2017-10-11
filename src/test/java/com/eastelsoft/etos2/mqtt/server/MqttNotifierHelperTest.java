package com.eastelsoft.etos2.mqtt.server;

import static org.junit.Assert.*;

import org.junit.Test;

import com.eastelsoft.etos2.mqtt.cluster.MqttNotifierHelper;

public class MqttNotifierHelperTest {

	@Test
	public void test() {
		MqttNotifierHelper.disconnect("10.0.65.105:8889", "59b65778e80f09cd06b9bba8");
//		MqttNotifierHelper.publish("10.0.65.105:8889", "identifier", new StoredMessage(Topic.asTopic("topic"), new byte[0], false, 1));
	}

}
