package com.eastelsoft.etos2.mqtt.server;

import java.io.Serializable;

public class Session implements Serializable {
	private String identifier;
	private String userName;
	private String serverId;// 连接的服务器id
	private String sessionId;// 会话id
	private long loginTime;// 登陆时间，毫秒数
	private String scope;// 最终的访问权限范围
	private String remoteAddress;// 请求端地址
	private boolean willMessageFlag = false;//mqtt will message
	private boolean cleanSessionFlag = true;//mqtt clean session 
	
	public Session() {
	}
	
	public Session(String identifier, String userName, String serverId,
			String sessionId) {
		this(identifier, userName, serverId, sessionId, System
				.currentTimeMillis());
	}

	public Session(String identifier, String userName, String serverId,
			String sessionId, long loginTime) {
		this.identifier = identifier;
		this.userName = userName;
		this.serverId = serverId;
		this.sessionId = sessionId;
		this.loginTime = loginTime;
	}

	public long getLoginTime() {
		return loginTime;
	}

	public void setLoginTime(long loginTime) {
		this.loginTime = loginTime;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public boolean isWillMessageFlag() {
		return willMessageFlag;
	}

	public void setWillMessageFlag(boolean willMessageFlag) {
		this.willMessageFlag = willMessageFlag;
	}

	public boolean isCleanSessionFlag() {
		return cleanSessionFlag;
	}

	public void setCleanSessionFlag(boolean cleanSessionFlag) {
		this.cleanSessionFlag = cleanSessionFlag;
	}
	
}
