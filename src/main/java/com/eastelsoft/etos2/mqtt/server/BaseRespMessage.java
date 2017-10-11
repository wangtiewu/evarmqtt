package com.eastelsoft.etos2.mqtt.server;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * 响应消息基础类
 * 
 * @author eastelsoft
 *
 */
public class BaseRespMessage extends BaseMessage implements Serializable {
	private String seq;// 消息序列号
	@XStreamOmitField
	private String _who;// 请求消息的帐号或网关id
	@XStreamOmitField
	private String _token;// 请求消息的令牌
	@XStreamOmitField
	private String _ip = "";// 发起请求消息的ip
	@XStreamOmitField
	private int _port;// 发起请求的端口
	@XStreamOmitField
	private long _keepAliveTime;//消息存活截止时间

	private String ecode = ErrorCode.ECODE_SUCCESS;// 错误码
	private String emsg = "成功";// 错误信息描述

	public BaseRespMessage() {
	}

	public BaseRespMessage(String ecode) {
		this.ecode = ecode;
		this.emsg = ErrorCode.getEmsg(ecode);
	}
	
	public BaseRespMessage(String ecode, String emsg) {
		this.ecode = ecode;
		this.emsg = emsg;
	}
	
	public BaseRespMessage withEcode(String ecode) {
		this.ecode = ecode;
		return this;
	}
	
	public BaseRespMessage withArguments(String... arguments) {
		this.emsg = ErrorCode.getEmsg(ecode, arguments);
		return this;
	}
	
	public String getSeq() {
		return seq;
	}

	public void setSeq(String seq) {
		this.seq = seq;
	}

	public String getEcode() {
		return ecode;
	}

	public void setEcode(String ecode) {
		this.ecode = ecode;
	}

	public String getEmsg() {
		return emsg;
	}

	public void setEmsg(String emsg) {
		this.emsg = emsg;
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
	public int get_port() {
		return _port;
	}

	@JsonIgnore
	public void set_port(int _port) {
		this._port = _port;
	}
	
	@JsonIgnore
	public long get_keepAliveTime() {
		return _keepAliveTime;
	}
	
	@JsonIgnore
	public void set_keepAliveTime(long _keepAliveTime) {
		this._keepAliveTime = _keepAliveTime;
	}

	public static BaseRespMessage fromJson(String json) {
		ObjectMapper mapper = new ObjectMapper();
		BaseRespMessage v = null;
		;
		try {
			v = mapper.readValue(json, BaseRespMessage.class);
			return v;
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return v;
	}
	
	public static String toJson(BaseRespMessage o) {
		ObjectMapper mapper = new ObjectMapper();
		StringWriter writer = null;
		JsonGenerator gen = null;

		try {
			writer = new StringWriter();
			gen = new JsonFactory().createGenerator(writer);
			mapper.writeValue(gen, o);
			String json = writer.toString();
			return json;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (gen != null) {
					gen.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	
}
