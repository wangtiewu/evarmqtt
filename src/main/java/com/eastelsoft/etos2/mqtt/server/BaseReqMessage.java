package com.eastelsoft.etos2.mqtt.server;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eet.evar.tool.IdGenerator;

public class BaseReqMessage extends BaseMessage {
	private String seq = IdGenerator.createObjectIdHex();// 消息序列号
	private long _reqTime = System.currentTimeMillis();// 消息请求时间
	private String _serverId;// 收到请求消息的服务器id，spanId
	private String _who;// 请求消息的帐号或网关id
	private String _token;// 请求消息的令牌
	private String _ip;// 发起请求消息的ip
	private int _port;// 发起请求消息的端口
	private String _api;// 接口消息名称
	
	public String getSeq() {
		return seq;
	}

	public void setSeq(String seq) {
		this.seq = seq;
	}

	@JsonIgnore
	public long get_reqTime() {
		return _reqTime;
	}

	@JsonIgnore
	public void set_reqTime(long _reqTime) {
		this._reqTime = _reqTime;
	}

	@JsonIgnore
	public String get_who() {
		return _who;
	}

	@JsonIgnore
	public void set_who(String _who) {
		this._who = _who;
	}

	@JsonIgnore
	public String get_token() {
		return _token;
	}

	@JsonIgnore
	public void set_token(String _token) {
		this._token = _token;
	}

	@JsonIgnore
	public String get_ip() {
		return _ip;
	}

	@JsonIgnore
	public void set_ip(String _ip) {
		this._ip = _ip;
	}

	@JsonIgnore
	public int get_port() {
		return _port;
	}

	@JsonIgnore
	public void set_port(int _port) {
		this._port = _port;
	}

	@JsonIgnore
	public String get_serverId() {
		return _serverId;
	}

	public void set_serverId(String _serverId) {
		this._serverId = _serverId;
	}

	@JsonIgnore
	public String get_api() {
		return _api;
	}

	@JsonIgnore
	public void set_api(String _api) {
		this._api = _api;
	}
}
