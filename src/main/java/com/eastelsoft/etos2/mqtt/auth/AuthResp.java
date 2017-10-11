package com.eastelsoft.etos2.mqtt.auth;

import com.eastelsoft.etos2.mqtt.server.BaseRespMessage;


/**
 * 认证返回消息
 * 
 * @author Eastelsoft
 *
 */
public class AuthResp extends BaseRespMessage {
	public AuthResp() {
		super();
	}
	public AuthResp(String ecode, String emsg) {
		super(ecode, emsg);
	}
}
