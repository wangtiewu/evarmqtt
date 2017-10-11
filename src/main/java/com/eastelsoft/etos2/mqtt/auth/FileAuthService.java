package com.eastelsoft.etos2.mqtt.auth;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import com.eastelsoft.etos2.mqtt.server.ErrorCode;
import com.eastelsoft.etos2.mqtt.server.Topic;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class FileAuthService implements AuthService {
	private final static Logger logger = LoggerFactory
			.getLogger(FileAuthService.class);
	private Map<String, String> passwdMap = new HashMap();
	private String passwdFile = "passwd";

	public void init() throws Exception {
		InputStream is = this.getClass().getClassLoader()
				.getResourceAsStream(passwdFile);
		if (is == null) {
			logger.error("passwd文件 {} classpath中不存在", passwdFile);
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				int delimiterIdx = line.indexOf(':');
				String username = line.substring(0, delimiterIdx).trim();
				String password = line.substring(delimiterIdx + 1).trim();
				passwdMap.put(username, password);
			}
		} finally {
			if (br != null) {
				br.close();
			}
			is.close();
		}
	}

	@Override
	public AuthResp auth(AuthReq authReq, String salt) {
		// TODO Auto-generated method stub
		if (StringUtils.isEmpty(authReq.getAppId()) || StringUtils.isEmpty(authReq.getAppKey())) {
			return new AuthResp(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR, ErrorCode.getEmsg(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR));
		}
		if (!passwdMap.containsKey(authReq.getAppId())) {
			return new AuthResp(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR, ErrorCode.getEmsg(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR));
		}
		String foundPwd = passwdMap.get(authReq.getAppId());
		String encodedPasswd = DigestUtils.sha256Hex(authReq.getAppKey());
		return foundPwd.equals(encodedPasswd) == true ? new AuthResp() : new AuthResp(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR, ErrorCode.getEmsg(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR));
	}

	@Override
	public boolean accessRateLimit(String appId, String capabilityId, String api) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean authorization(String appId, String capabilityId, String api) {
		// TODO Auto-generated method stub
		return true;
	}

	public String getPasswdFile() {
		return passwdFile;
	}

	public void setPasswdFile(String passwdFile) {
		this.passwdFile = passwdFile;
	}

	@Override
	public boolean canSub(String userName, String identifier, Topic topic, int qos) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canPub(String userName, String identifier, Topic topic, int qos) {
		// TODO Auto-generated method stub
		return true;
	}

}
