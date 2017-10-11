package com.eastelsoft.etos2.mqtt.util;

import com.eastelsoft.etos2.mqtt.server.RedisKeyBuilder;

import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;

public class AccessLimit {
	private static Redis redis = (Redis) EvarBeanFactory.instance().makeBean(
			"redis");

	/**
	 * 判断访问是否超限,true超过，false未超过 方法第一个参数为客户端ip或者token 并且访问次数加1
	 * 
	 * @param ipOrAppId
	 * @param uri
	 * @param limitTimes
	 * @return
	 */
	public static boolean incAndCheckAccessCount(String ipOrAppId, String uri,
			int limitTimes) {
		if (inWhiteList(ipOrAppId)) {
			return false;
		}
		// incrby增加key对应value，没有对应key则创建key并赋值1
		long count = redis.incrby(
				RedisKeyBuilder.buildAccessLimitKey(ipOrAppId, uri), 1);
		if (count == 1) {
			// 对新建的key设置过期时间
			redis.expire(RedisKeyBuilder.buildAccessLimitKey(ipOrAppId, uri),
					RedisKeyBuilder.REDIS_KEY_AL_TIMEOUT);
			return false;
		}
		if (count > limitTimes) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean inWhiteList(String ip) {
		if (true) {
			return true;
		}
		if (ip.equals("127.0.0.1")) {
			return true;
		}
		return IpListLoader.getInstance().getIpList().contains(ip);
	}
}
