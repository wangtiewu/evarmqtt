package com.eastelsoft.etos2.mqtt.util;

import java.util.List;

import eet.evar.core.redis.Redis;
import eet.evar.tool.AbsBatchProcess;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class RedisBatchProcUtil {
	private static final Logger logger = LoggerFactory
			.getLogger(RedisBatchProcUtil.class);
	protected static Redis redis;
	private static RedisBatchDel redisBatchDel;
	private static RedisBatchTtl redisBatchTtl;

	public static void start() {
		if (redisBatchDel == null) {
			synchronized (RedisBatchProcUtil.class) {
				if (redisBatchDel == null) {
					redisBatchDel = new RedisBatchDel();
					redisBatchDel.start();
				}
			}
		}
		if (redisBatchTtl == null) {
			synchronized (RedisBatchProcUtil.class) {
				if (redisBatchTtl == null) {
					redisBatchTtl = new RedisBatchTtl();
					redisBatchTtl.start();
				}
			}
		}
	}

	public static void stop() {
		if (redisBatchDel != null) {
			redisBatchDel.stop();
			redisBatchDel = null;
		}
		if (redisBatchTtl != null) {
			redisBatchTtl.stop();
			redisBatchTtl = null;
		}
	}

	public static void setRedis(Redis redis) {
		RedisBatchProcUtil.redis = redis;
	}

	public static void submitDel(String... key) {
		redisBatchDel.batch(key);
	}

	public static void submitTtt(String key, long timeout) {
		redisBatchTtl.batch(new TtlOper(key, timeout));
	}
}

class RedisBatchDel extends AbsBatchProcess<Object> {
	private static final Logger logger = LoggerFactory
			.getLogger(RedisBatchDel.class);

	public RedisBatchDel() {
		super(200, 200, 5000);
	}

	@Override
	protected void process(List<Object> arg0) {
		// TODO Auto-generated method stub
		if (arg0.isEmpty()) {
			return;
		}
		long count = RedisBatchProcUtil.redis.mdelete(arg0
				.toArray(new String[0]));
		logger.info("实际删除key数量{}，预期删除key数量 {}", count, arg0.size());
	}

}

class TtlOper {
	private String key;
	private long timeout;

	public TtlOper(String key, long timeout) {
		this.key = key;
		this.timeout = timeout;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

}

class RedisBatchTtl extends AbsBatchProcess<TtlOper> {
	private static final Logger logger = LoggerFactory
			.getLogger(RedisBatchDel.class);

	public RedisBatchTtl() {
		super(200, 200, 5000);
	}

	@Override
	protected void process(List<TtlOper> arg0) {
		// TODO Auto-generated method stub
		if (arg0.isEmpty()) {
			return;
		}
		String[] keys = new String[arg0.size()];
		long[] vals = new long[arg0.size()];
		for(int i = 0; i<arg0.size(); i++) {
			keys[i] = arg0.get(i).getKey();
			vals[i] = arg0.get(i).getTimeout();
		}
		RedisBatchProcUtil.redis.expire(keys, vals);
		logger.info("完成批量Ttl操作，数量 {}", arg0.size());
	}

}
