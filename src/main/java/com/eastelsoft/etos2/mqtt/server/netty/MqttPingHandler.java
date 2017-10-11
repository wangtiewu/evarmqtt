package com.eastelsoft.etos2.mqtt.server.netty;

import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

import java.util.List;
import java.util.Map;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.MqttConsts;
import com.eastelsoft.etos2.mqtt.server.RedisKeyBuilder;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;

import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttPingHandler extends MqttMessageHandler<MqttMessage> {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttPingHandler.class);
	private final Redis redis = (Redis) EvarBeanFactory.instance().makeBean(
			"redis");

	@Override
	public void onData(Client client, Map<String, List<String>> parameters,
			MqttMessage data) throws Exception {
		// TODO Auto-generated method stub
//		IntfLoggerUtil.logInboundMessage(logger,
//				MqttMessageType.PINGREQ.name(), client, data.toString());
		MqttFixedHeader pingHeader = new MqttFixedHeader(
				MqttMessageType.PINGRESP, false, AT_MOST_ONCE, false, 0);
		MqttMessage pingResp = new MqttMessage(pingHeader);
		client.send(MqttMessageType.PINGRESP.name(), pingResp);
//		IntfLoggerUtil.logOutboundMessage(logger,
//				MqttMessageType.PINGRESP.name(), client, pingResp.toString());
		client.lastKeepAliveTime((int)(System.currentTimeMillis()/1000));

	}
}
