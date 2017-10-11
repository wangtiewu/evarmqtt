package com.eastelsoft.etos2.mqtt.auth;

import com.eastelsoft.etos2.mqtt.server.BaseReqMessage;

/**
 * 认证请求
 * 
 * @author Eastelsoft
 *
 */
public class AuthReq extends BaseReqMessage {
	private String appId;// 应用id
	private String appKey;// 应用key
	private String scope;// 权限
	private String ip;// 请求id地址

	public AuthReq(String appId, String appKey, String scope) {
		this.appId = appId;
		this.appKey = appKey;
		this.scope = scope;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

}
