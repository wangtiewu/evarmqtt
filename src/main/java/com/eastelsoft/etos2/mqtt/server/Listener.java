package com.eastelsoft.etos2.mqtt.server;


/**
 * web平台统计用的监听器接口
 * 
 * @author eastelsoft
 *
 */
public interface Listener {
	/**
	 * 
	 * @param uri
	 *            对应业务接口名
	 * @param requestTime
	 *            请求时间
	 * @param requestData
	 *            客户端上报的数据
	 * @param resultData
	 *            服务端返回的数据
	 */
	public void onEvent(Client client, String uri, Object requestData,
			Object resultData);

}
