package com.eastelsoft.etos2.mqtt.util;

import com.eastelsoft.etos2.mqtt.server.Client;

import eet.evar.tool.logger.Logger;

public class IntfLoggerUtil {
	public static void logInboundMessage(Logger logger, String uri, Client client, String data) {
		StringBuffer sb = new StringBuffer(uri);
		sb.append("(client->broker)：");
		sb.append("sessionId=").append(client.getSessionId()).append(" ");
		if (client.getSession() != null) {
			sb.append("identifier=").append(client.getSession().getIdentifier()).append(" ");
		}
		sb.append("remoteAddress=").append(client.getRemoteAddress()).append(" ");
		sb.append("data=").append(data);
		logger.info(sb.toString());
	}
	
	public static void logOutboundMessage(Logger logger, String uri, Client client, String data) {
		StringBuffer sb = new StringBuffer(uri);
		sb.append("(broker->client)：");
		sb.append("sessionId=").append(client.getSessionId()).append(" ");
		if (client.getSession() != null) {
			sb.append("identifier=").append(client.getSession().getIdentifier()).append(" ");
		}
		sb.append("remoteAddress=").append(client.getRemoteAddress()).append(" ");
		sb.append("data=").append(data);
		logger.info(sb.toString());
	}
}
