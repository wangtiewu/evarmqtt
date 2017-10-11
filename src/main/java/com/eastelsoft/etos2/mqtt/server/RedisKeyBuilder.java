package com.eastelsoft.etos2.mqtt.server;

import java.io.Serializable;

import eet.evar.tool.MD5;

/**
 * Redis key 生成器
 * 
 * @author eastelsoft
 *
 */
public class RedisKeyBuilder {
	private final static String REDIS_KEY_SEPARATOR = "_";
	private final static String REDIS_KEY_ACCESS_LIMIT = "al_";// 访问次数限制（ip/token）
	public final static long REDIS_KEY_AL_TIMEOUT = 60L * 60 * 1000;
	private final static String REDIS_KEY_SESSION = "mqtt.session:";// 会话key前缀
	private final static String REDIS_KEY_RETAINMSGS = "mqtt.retainmsgs";// map<Identifier, StoredMessage>
	
	/**
	 * 访问次数限制key 第一个参数是客户端ip或者信息上传的token
	 * 
	 * @param ipOToken
	 * @param uri
	 * @return
	 */
	public static String buildAccessLimitKey(String ipOToken, String uri) {
		MD5 md5 = new MD5(ipOToken + uri);
		return REDIS_KEY_ACCESS_LIMIT + md5.asHex();
	}

	/**
	 * 广播队列
	 * 
	 * @param namespaceName
	 *            命名空间
	 * @param who
	 *            成员
	 * @return
	 */
	public static String buildBroadcastQueueKey(String namespaceName, String who) {
		// TODO Auto-generated method stub
		return "broadcast:" + namespaceName + "." + who;
	}

	/**
	 * 会话key
	 * @param identifier
	 * @return
	 */
	public static String buildSessionKey(String identifier) {
		return REDIS_KEY_SESSION + identifier;
	}
	
	/**
	 * retain messages key
	 * @param identifier
	 * @return
	 */
	public static String buildRetainMsgsKey(String identifier) {
		return REDIS_KEY_RETAINMSGS;
	}
}
