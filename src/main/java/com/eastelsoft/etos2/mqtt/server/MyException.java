package com.eastelsoft.etos2.mqtt.server;

public class MyException extends RuntimeException {
	private String ecode;
	private String emsg;

	public MyException(String ecode, String emsg) {
		this.ecode = ecode;
		this.emsg = emsg;
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
	
}
