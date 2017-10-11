package com.eastelsoft.etos2.mqtt.server.netty;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

public class MqttProtoHelper {
	public static int messageId(MqttMessage msg) {
		if (msg instanceof MqttPublishMessage) {
			 return ((MqttPublishMessage) msg).variableHeader().messageId();
		}
		return ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId();
	}
}
