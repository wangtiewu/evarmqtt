package com.eastelsoft.etos2.mqtt.server.netty;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

import java.util.List;
import java.util.Map;

import com.eastelsoft.etos2.mqtt.server.Client;
import com.eastelsoft.etos2.mqtt.server.Client.ConnectionCloseType;
import com.eastelsoft.etos2.mqtt.server.WillMessageStore;
import com.eastelsoft.etos2.mqtt.util.IntfLoggerUtil;

import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class MqttDisconnectHandler extends MqttMessageHandler<MqttMessage> {
	private static final Logger logger = LoggerFactory
			.getLogger(MqttDisconnectHandler.class);
	private static final WillMessageStore willMessageStore = (WillMessageStore) EvarBeanFactory.instance().makeBean(
			"willMessageStore");

	@Override
	public void onData(Client client, Map<String, List<String>> parameters,
			MqttMessage data) throws Exception {
		// TODO Auto-generated method stub
		IntfLoggerUtil.logInboundMessage(logger, MqttMessageType.DISCONNECT.name(), client, data.toString());
		willMessageStore.remove(client.getSession().getIdentifier());
		client.assignConnCloseType(ConnectionCloseType.PROTO_DISCONNECTED);
		client.disconnect();
		logger.info("{} 关闭连接（收到Disconnect消息）：dentifier={}，remoteAddress={}",
				client.getSession().getSessionId(),
				client.getSession().getIdentifier(),
				client.getSession().getRemoteAddress());

	}
}
