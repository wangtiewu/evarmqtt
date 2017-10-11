package com.eastelsoft.etos2.mqtt.auth;

import com.eastelsoft.etos2.mqtt.server.Topic;


/**
 * 认证服务
 * 
 * @author Eastelsoft
 *
 */
public interface AuthService {
	/**
	 * 认证
	 * 
	 * @param authReq
	 * @param salt
	 *            混淆器，填空表示salt不参与MD5计算，不为空时参与MD5计算：MD5(salt+MD5(appKey))
	 * @return 根据ecode判断认证是否成功
	 */
	AuthResp auth(AuthReq authReq, String salt);

	/**
	 * 接口访问频率限制，每访问一次加1
	 * 
	 * @param appId		 应用id
	 * @param capabilityId 能力id
	 * @param api		 能力接口
	 * @return true，已超出限制；false，未超出
	 */
	boolean accessRateLimit(String appId, String capabilityId, String api);

	/**
	 * 接口访问授权
	 * 
	 * @param appId 应用id
	 * @param capabilityId 能力id
	 * @param api 能力接口
	 * @return true，已授权；false，未授权
	 */
	boolean authorization(String appId, String capabilityId, String api);

	/**
	 * 是否可以订阅该主题
	 * @param userName
	 * @param identifier
	 * @param topic
	 * @param qos
	 * @return
	 */
	boolean canSub(String userName, String identifier, Topic topic, int qos);
	
	/**
	 * 是否可以发布该主题
	 * @param userName
	 * @param identifier
	 * @param topic
	 * @param qos
	 * @return
	 */
	boolean canPub(String userName, String identifier, Topic topic, int qos);
}
