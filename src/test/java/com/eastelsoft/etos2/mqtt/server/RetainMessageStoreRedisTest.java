package com.eastelsoft.etos2.mqtt.server;

import static org.junit.Assert.*;

import org.junit.Test;

import com.eastelsoft.etos2.rpc.tool.JacksonUtils;

import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;

public class RetainMessageStoreRedisTest {

	@Test
	public void test() {
		EvarBeanFactory
				.instance(new String[] { "spring-bean-container-cache.xml" });
		RetainMessageStoreRedis retainMsgStore = new RetainMessageStoreRedis();
		Redis redis = (Redis) EvarBeanFactory.instance().makeBean("redis");
		retainMsgStore.setRedis(redis);
		redis.mapSet("a", "b", "c");

		Topic pubTopic = Topic.asTopic("ab/cd/ef");
		String payload = "abcdef";
		StoredMessage retainMessage = new StoredMessage(pubTopic,
				payload.getBytes(), true, 0, "");
		System.out.println(JacksonUtils.bean2Json(retainMessage));
		String json = JacksonUtils.bean2Json(retainMessage);
		retainMessage = JacksonUtils.json2Bean(json, StoredMessage.class);
		redis.set("Test_abc", retainMessage);
		StoredMessage retainMessage2 = redis.get("Test_abc", StoredMessage.class);
		redis.delete("Test_abc");
		retainMsgStore.put(pubTopic, retainMessage);

		Topic pubTopic1 = Topic.asTopic("1/cd/ef");
		String payload1 = "abcdef";
		StoredMessage retainMessage1 = new StoredMessage(pubTopic1,
				payload1.getBytes(), true, 0, "");
		retainMsgStore.put(pubTopic1, retainMessage1);

		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/cd/ef")).size());
		assertEquals(2, retainMsgStore.match(Topic.asTopic("#")).size());
		assertEquals(2, retainMsgStore.match(Topic.asTopic("+/cd/ef")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/#")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/+/ef")).size());
		assertEquals(2, retainMsgStore.match(Topic.asTopic("+/+/ef")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/cd/#")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/cd/+")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/+/+")).size());
		assertEquals(2, retainMsgStore.match(Topic.asTopic("+/cd/+")).size());
		assertEquals(2, retainMsgStore.match(Topic.asTopic("+/+/+")).size());
		
		retainMsgStore.remove(pubTopic1);
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/cd/ef")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("#")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("+/cd/ef")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/#")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/+/ef")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("+/+/ef")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/cd/#")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/cd/+")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("ab/+/+")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("+/cd/+")).size());
		assertEquals(1, retainMsgStore.match(Topic.asTopic("+/+/+")).size());
		
		retainMsgStore.remove(pubTopic);
		assertEquals(0, retainMsgStore.match(Topic.asTopic("ab/cd/ef")).size());
		assertEquals(0, retainMsgStore.match(Topic.asTopic("#")).size());
		assertEquals(0, retainMsgStore.match(Topic.asTopic("+/cd/ef")).size());
		assertEquals(0, retainMsgStore.match(Topic.asTopic("ab/#")).size());
		assertEquals(0, retainMsgStore.match(Topic.asTopic("ab/+/ef")).size());
		assertEquals(0, retainMsgStore.match(Topic.asTopic("+/+/ef")).size());
		assertEquals(0, retainMsgStore.match(Topic.asTopic("ab/cd/#")).size());
		assertEquals(0, retainMsgStore.match(Topic.asTopic("ab/cd/+")).size());
		assertEquals(0, retainMsgStore.match(Topic.asTopic("ab/+/+")).size());
		assertEquals(0, retainMsgStore.match(Topic.asTopic("+/cd/+")).size());
		assertEquals(0, retainMsgStore.match(Topic.asTopic("+/+/+")).size());
	}

}
