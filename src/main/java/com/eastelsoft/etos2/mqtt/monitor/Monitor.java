package com.eastelsoft.etos2.mqtt.monitor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

import com.bealetech.metrics.reporting.Statsd;
import com.bealetech.metrics.reporting.StatsdReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import eet.evar.StringDeal;
import eet.evar.core.redis.Redis;
import eet.evar.framework.bean.EvarBeanFactory;
import eet.evar.tool.BloomFilterRedis;
import eet.evar.tool.DateFormat;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

/**
 * 
 * @author eastelsoft
 *
 */

public class Monitor {
	public static final String SVR_DB_TABLESPACE_LOW = "数据库表空间不足";
	public static final String SVR_DB_CONNECTION_INVALID = "数据库连接异常";
	public static final String SVR_REDIS_MEM_HIGHT = "redis内存使用率过高";
	public static final String SVR_REDIS_CONNECTION_INVALID = "redis连接异常";
	private static final Logger logger = LoggerFactory.getLogger(Monitor.class);
	static final String STAT_INTF_PRE = "etos.";
	static final String INTF_ALL = "all";
	static final String INTF_REQUEST_COUNT = "count";
	static final String INTF_REQUEST_DEALY = "delay";
	static final int REPORT_CYCLE = 5;// 单位秒
	static final String[] hours = new String[] { "00", "01", "02", "03", "04",
			"05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15",
			"16", "17", "18", "19", "20", "21", "22", "23" };
	Redis redis = (Redis) EvarBeanFactory.instance().makeBean("redis");
	MetricRegistry metrics = new MetricRegistry();
	volatile long start = System.currentTimeMillis();
	JmxReporter jmxReporter = null;
	Statsd statsd = null;
	StatsdReporter reporter = null;
	
	public Monitor() {
		this("");
	}

	public Monitor(String gangliaServer) {
		if (gangliaServer != null && !gangliaServer.equals("")) {
			String[] serverPort = StringDeal.split(gangliaServer, ":");
			String host = "localhost";
			int port = 8649;
			if (serverPort.length == 1) {
				host = serverPort[0];
			} else if (serverPort.length == 2) {
				host = serverPort[0];
				port = Integer.parseInt(serverPort[1]);
			}
			startStatsdReporter(host, port);
		}
		startJMXReporter();
		startReportGauge();
		logger.info("monitor has started");
	}

	public void stop() {
		if (jmxReporter != null) {
			jmxReporter.stop();
			jmxReporter = null;
		}
		if (reporter != null) {
			reporter.stop();
			reporter = null;
		}
		if (statsd != null) {
			try {
				statsd.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			statsd = null;
		}
		logger.info("monitor has stoped");
	}

	private void startReportGauge() {
	}

	private void startJMXReporter() {
		jmxReporter = JmxReporter.forRegistry(metrics).build();
		jmxReporter.start();
	}

	private void startStatsdReporter(String host, int port) {
		statsd = new Statsd(host, port);
		reporter = StatsdReporter.forRegistry(metrics)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.convertRatesTo(TimeUnit.SECONDS).filter(MetricFilter.ALL)
				.build(statsd);
		reporter.start(REPORT_CYCLE, TimeUnit.SECONDS);
		logger.info("Statsd reporter started，host:{0}, port:{1}, 上报周期{2}秒",
				host, "" + port, REPORT_CYCLE);
	}

	/**
	 * 检查业务是否正常
	 */
	public void checkHealth() {
		checkRedisHealth();
	}

	/**
	 * 检查数据库是否正常
	 */
	private void checkDbHealth() {
		// TODO Auto-generated method stub
		Properties jdbcProperties = getJdbcProperties();
		if (jdbcProperties == null) {
			logger.error("checkDbHealth失败：jdbc.properties不存在");
			return;
		}
		String dbUrl = jdbcProperties.getProperty("jdbc.url");
		if (StringUtils.isEmpty(dbUrl)) {
			logger.error("checkDbHealth失败：jdbc.properties未配置jdbc.url参数");
			return;
		}
		if (dbUrl.toLowerCase().contains("oracle")) {
			String[] spilit1 = StringDeal.split(dbUrl, "@");
			if (spilit1.length < 2) {
				logger.error("checkDbHealth失败：jdbc.properties配置jdbc.url参数格式错误");
				return;
			}
			spilit1 = StringDeal.split(spilit1[1], ":");
			if (spilit1.length < 2) {
				logger.error("checkDbHealth失败：jdbc.properties配置jdbc.url参数格式错误");
				return;
			}
			String ip = spilit1[0];
			if (ip.startsWith("//")) {
				ip = ip.substring(2);
			}
			checkDBConnectionHealth("oracle", ip);
			checkOracleTableSpaces(ip);
		} else if (dbUrl.toLowerCase().contains("mysql")) {
			// jdbc:mysql://10.0.65.13:3306/etos_v2?characterEncoding=UTF-8
			// jdbc:oracle:thin:@//211.140.7.179:8098/sitdb
			String[] spilit1 = StringDeal.split(dbUrl, "?");
			if (spilit1.length < 2) {
				logger.error("checkDbHealth失败：jdbc.properties配置jdbc.url参数格式错误");
				return;
			}
			spilit1 = StringDeal.split(spilit1[0], ":");
			if (spilit1.length < 4) {
				logger.error("checkDbHealth失败：jdbc.properties配置jdbc.url参数格式错误");
				return;
			}
			String ip = spilit1[2];
			if (ip.startsWith("//")) {
				ip = ip.substring(2);
			}
			checkDBConnectionHealth("mysql", ip);
			// checkOracleTableSpaces(ip);
		} else {
			logger.error("checkDbHealth失败：目前只支持oracle数据库");
			return;
		}
	}

	private void checkDBConnectionHealth(String db, String ip) {
		// TODO Auto-generated method stub
		String sql = "select 1 from dual";
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			if (rs.next()) {
				clearAlater(db + "_" + ip, SVR_DB_CONNECTION_INVALID, ip,
						"数据库连接已恢复正常");
			}
		} catch (Exception e) {
			logger.error("查询" + db + "失败", e);
			createAlater(db + "_" + ip, SVR_DB_CONNECTION_INVALID, ip, "",
					new Date(), "2", "数据库连接异常：" + e.getMessage());
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				rs = null;
			}
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void checkOracleTableSpaces(String ip) {
		// TODO Auto-generated method stub
		String sql = "SELECT df.tablespace_name||'-'||(sysdate) tbs_timeid ,df.tablespace_name||'-'||(sysdate-1) ys_tbs_timeid,df.tablespace_name"
				+ ",COUNT(*) datafile_count,ROUND(SUM(df.BYTES) / 1048576) size_mb"
				+ ",ROUND(SUM(free.BYTES) / 1048576, 2) free_mb"
				+ ",ROUND(SUM(df.BYTES) / 1048576 - SUM(free.BYTES) / 1048576, 2) used_mb"
				+ ",ROUND(MAX(free.maxbytes) / 1048576, 2) maxfree"
				+ ",100 - ROUND(100.0 * SUM(free.BYTES) / SUM(df.BYTES), 2) pct_used"
				+ ",ROUND(100.0 * SUM(free.BYTES) / SUM(df.BYTES), 2) pct_free,(sysdate) time "
				+ "FROM dba_data_files df"
				+ ",(SELECT tablespace_name,file_id,SUM(BYTES) BYTES,MAX(BYTES) maxbytes "
				+ "FROM dba_free_space "
				+ "GROUP BY tablespace_name, file_id) free "
				+ "WHERE df.tablespace_name = free.tablespace_name(+) "
				+ "AND df.file_id = free.file_id(+) "
				+ "GROUP BY df.tablespace_name " + "ORDER BY 8";
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			while (rs.next()) {
				float totalMB = rs.getFloat(5);
				float freeMB = rs.getFloat(6);
				float pctFree = rs.getFloat(10);
				float freeLowLimit = 20;
				if (pctFree < freeLowLimit) {
					// 空闲率低于阀值告警
					createAlater("oracle_" + ip, SVR_DB_TABLESPACE_LOW + "_"
							+ rs.getString(3), ip, "", new Date(), "2", "表空间"
							+ rs.getString(3) + "空闲率" + pctFree + "，小于告警阀值"
							+ freeLowLimit + "，剩余空间" + freeMB + "MB");
				} else if (pctFree > freeLowLimit * 1.2) {
					// 清除空闲率低于阀值告警
					clearAlater("oracle_" + ip, SVR_DB_TABLESPACE_LOW + "_"
							+ rs.getString(3), ip, "表空间" + rs.getString(3)
							+ "空闲率已恢复正常，当前值为" + pctFree + "，剩余空间" + freeMB
							+ "MB");
				}
			}
		} catch (Exception e) {
			logger.error("查询oracle表空间失败", e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				rs = null;
			}
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 检查redis是否正常
	 */
	private void checkRedisHealth() {
		// TODO Auto-generated method stub
		Properties redisProperties = getRedisProperties();
		if (redisProperties == null) {
			logger.error("checkRedisHealth失败：redis.properties不存在");
			return;
		}
		String redisServer = redisProperties.getProperty("redis.server");
		if (StringUtils.isEmpty(redisServer)) {
			logger.error("checkRedisHealth失败：redis.properties未配置redis.server参数");
			return;
		}
		String ip = redisServer.substring(0, redisServer.indexOf(":"));
		List<Float> ratios = redis.memRatio();
		if (ratios == null || ratios.isEmpty()) {
			createAlater("redis_" + ip, SVR_REDIS_CONNECTION_INVALID, ip, "",
					new Date(), "2", "redis连接异常");
		} else {
			clearAlater("redis_" + ip, SVR_REDIS_CONNECTION_INVALID, ip,
					"redis连接恢复正常");
		}
		float ratioHightQuota = 80;
		for (Float ratio : ratios) {
			if (ratio > ratioHightQuota) {
				createAlater("redis_" + ip, SVR_REDIS_MEM_HIGHT, ip, "",
						new Date(), "2", "redis内存使用率" + ratio + "，超过告警阀值"
								+ ratioHightQuota);
			} else if (ratio < ratioHightQuota * 0.85) {
				clearAlater("redis_" + ip, SVR_REDIS_MEM_HIGHT, ip,
						"redis内存使用率已恢复正常，当前值为" + ratio);
			}
		}
	}

	private boolean isAlater(String moduleId, String svrName, String ip) {
		return false;
	}

	private void clearAlater(String moduleId, String svrName, String ip,
			String clearInfo) {
	}

	private void createAlater(String moduleId, String svrName, String ip,
			String hostName, Date alterTime, String faultType, String faultInfo) {
	}

	/**
	 * 重置所有指标
	 */
	private synchronized void resetAllMeters() {
		// TODO Auto-generated method stub
		if (!isNewDay()) {
			return;
		}
		logger.info("重置监控指标完成");
	}

	/**
	 * 是否是新的一天
	 * 
	 * @return
	 */
	private boolean isNewDay() {
		long end = System.currentTimeMillis();
		long days = (end - start) / (1000 * 60 * 60 * 24);
		// 运行天数从1开始计数
		long runningDays = days;
		// 判断是否跨天，若跨天，运行天数还要+1
		long probableEndMillis = start + (1000 * 60 * 60 * 24) * days;
		if (new Date(probableEndMillis).getDay() != new Date(end).getDay()) {
			runningDays++;
		}
		return runningDays > 0 ? true : false;
	}

	/**
	 * 接口性能统计
	 * 
	 * @param serverId
	 *            服务器id
	 * @param uri
	 *            接口名称
	 * @param success
	 *            调用是否成功
	 */
	public void metricsCount(String serverId, String uri, boolean success) {
		// 记录调用次数
		Meter requests = metrics.meter(STAT_INTF_PRE + serverId + "."
				+ INTF_REQUEST_COUNT + "." + uri);
		requests.mark();
		requests = metrics.meter(STAT_INTF_PRE + serverId + "."
				+ INTF_REQUEST_COUNT + "." + INTF_ALL);
		requests.mark();
	}

	public void metricsDelay(String serverId, String uri, long delayMilliSeconds) {
		Timer timers = metrics.timer(STAT_INTF_PRE + serverId + "."
				+ INTF_REQUEST_DEALY + "." + uri);
		timers.update(delayMilliSeconds, TimeUnit.MILLISECONDS);
	}

	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		EvarBeanFactory.instance(new String[] {
				"spring-bean-container-cache.xml",
				"spring-bean-container-datasource.xml",
				"spring-bean-container-mqtt.xml" });
		eet.evar.tool.Logger.instance();
		Monitor monitor = new Monitor();
		for (int i = 0; i < 1000; i++) {
			// monitor.checkHealth();
			// Thread.sleep(1000);
		}
		for (int i = 0; i < 1000; i++) {
			monitor.metricsCount("1", "CONNECT", true);
			monitor.metricsDelay("1", "CONNACK", 1);
		}
		Thread.sleep(100000);

	}

	private String getAlertEMailAddress() {
		return null;
	}

	private Properties getJdbcProperties() {
		return getProperties("jdbc.properties");
	}

	private Properties getRedisProperties() {
		return getProperties("redis.properties");
	}

	private Properties getProperties(String fileName) {
		InputStream is = null;
		try {
			URL url = Monitor.class.getResource('/' + fileName);
			try {
				is = new FileInputStream(URLDecoder.decode(url.getFile(),
						"UTF-8"));
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Properties properties = new Properties();
			try {
				properties.load(is);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return properties;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		return ((DataSource) EvarBeanFactory.instance().makeBean("dataSource"))
				.getConnection();
	}

	private class FlowAlertProfile {
		double alertQuota;

		double resumeQuota;

		String beginTime;// 格式HHMI

		String endTime;// 格式HHMI

		public double getAlertQuota() {
			return alertQuota;
		}

		public void setAlertQuota(double alertQuota) {
			this.alertQuota = alertQuota;
		}

		public double getResumeQuota() {
			return resumeQuota;
		}

		public void setResumeQuota(double resumeQuota) {
			this.resumeQuota = resumeQuota;
		}

		public String getBeginTime() {
			return beginTime;
		}

		public void setBeginTime(String beginTime) {
			this.beginTime = beginTime;
		}

		public String getEndTime() {
			return endTime;
		}

		public void setEndTime(String endTime) {
			this.endTime = endTime;
		}

		public boolean atAlertTime() {
			if (StringUtils.isEmpty(beginTime) || StringUtils.isEmpty(endTime)) {
				return false;
			}
			if (!DateFormat.isDate(beginTime, "HHmm")
					|| !DateFormat.isDate(endTime, "HHmm")) {
				logger.error("开始时间 {0} 或 结束时间 {1} 格式错误，正确格式HHmm", beginTime,
						endTime);
				return false;
			}
			int curMin = Integer.parseInt(DateFormat
					.getNowByFormatString("HHmm"));
			if (Integer.parseInt(beginTime) > Integer.parseInt(endTime)) {
				if (!(curMin >= Integer.parseInt(beginTime) || curMin <= Integer
						.parseInt(endTime))) {
					return false;
				} else {
					return true;
				}
			} else {
				if (curMin < Integer.parseInt(beginTime)
						|| curMin > Integer.parseInt(endTime)) {
					return false;
				} else {
					return true;
				}
			}
		}

	}
}
