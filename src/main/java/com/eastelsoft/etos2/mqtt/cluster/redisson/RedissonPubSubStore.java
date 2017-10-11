package com.eastelsoft.etos2.mqtt.cluster.redisson;

import io.netty.util.internal.PlatformDependent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import com.eastelsoft.etos2.mqtt.cluster.PubSubListener;
import com.eastelsoft.etos2.mqtt.cluster.PubSubStore;
import com.eastelsoft.etos2.mqtt.cluster.PubSubType;
import com.eastelsoft.etos2.mqtt.cluster.message.PubSubMessage;

public class RedissonPubSubStore implements PubSubStore {

    private final RedissonClient redissonPub;
    private final RedissonClient redissonSub;
    private final Long nodeId;

    private final ConcurrentMap<String, Queue<Integer>> map = PlatformDependent.newConcurrentHashMap();

    public RedissonPubSubStore(RedissonClient redissonPub, RedissonClient redissonSub, Long nodeId) {
        this.redissonPub = redissonPub;
        this.redissonSub = redissonSub;
        this.nodeId = nodeId;
    }

    @Override
    public void publish(PubSubType type, PubSubMessage msg) {
        msg.setNodeId(nodeId);
        redissonPub.getTopic(type.toString()).publish(msg);
    }

    @Override
    public <T extends PubSubMessage> void subscribe(PubSubType type, final PubSubListener<T> listener, Class<T> clazz) {
        String name = type.toString();
        RTopic<T> topic = redissonSub.getTopic(name);
        int regId = topic.addListener(new MessageListener<T>() {
            @Override
            public void onMessage(String channel, T msg) {
                if (!nodeId.equals(msg.getNodeId())) {
                    listener.onMessage(msg);
                }
            }
        });

        Queue<Integer> list = map.get(name);
        if (list == null) {
            list = new ConcurrentLinkedQueue<Integer>();
            Queue<Integer> oldList = map.putIfAbsent(name, list);
            if (oldList != null) {
                list = oldList;
            }
        }
        list.add(regId);
    }

    @Override
    public void unsubscribe(PubSubType type) {
        String name = type.toString();
        Queue<Integer> regIds = map.remove(name);
        RTopic<Object> topic = redissonSub.getTopic(name);
        for (Integer id : regIds) {
            topic.removeListener(id);
        }
    }

    @Override
    public void shutdown() {
    }

}
