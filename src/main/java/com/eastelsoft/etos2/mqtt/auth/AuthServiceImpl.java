package com.eastelsoft.etos2.mqtt.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

import com.eastelsoft.etos2.mqtt.auth.AuthConsts.CAPABILITY;
import com.eastelsoft.etos2.mqtt.auth.AuthConsts.USER_STATUS;
import com.eastelsoft.etos2.mqtt.server.ErrorCode;
import com.eastelsoft.etos2.mqtt.server.Topic;
import com.eastelsoft.etos2.mqtt.util.AccessLimit;

import eet.evar.base.VerifyHelper;
import eet.evar.core.cache.Cache;
import eet.evar.core.redis.Redis;
import eet.evar.tool.MD5;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class AuthServiceImpl implements AuthService {
	private static final Logger logger = LoggerFactory
			.getLogger(AuthServiceImpl.class);
	private Cache localCache;
	private Redis ssoRedis;
	private DataSource dataSource;

	@Override
	public AuthResp auth(AuthReq authReq, String salt) {
		// TODO Auto-generated method stub
		AppCapability appCapability = findAppCapability(authReq.getAppId(),
				authReq.getScope());
		if (appCapability.isNull) {
			logger.error("auth 失败，应用 {0} 不存在或能力 {} 未开通", authReq.getAppId(), authReq.getScope());
			return new AuthResp(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR,
					ErrorCode.getEmsg(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR));
		}
		if (appCapability.getStatus() == USER_STATUS.STOPED) {
			logger.error("auth 失败，应用 {0} 授权的能力 {1} 已经停机", authReq.getAppId(),
					authReq.getScope());
			return new AuthResp(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR,
					ErrorCode.getEmsg(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR));
		}
		if (!appCapability.isValid()) {
			logger.error("auth 失败，应用 {0} 已无效", authReq.getAppId());
			return new AuthResp(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR,
					ErrorCode.getEmsg(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR));
		}
		if (appCapability.isLocked()) {
			logger.error("auth 失败，应用 {0} 已被锁定", authReq.getAppId());
			return new AuthResp(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR,
					ErrorCode.getEmsg(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR));
		}
		if (StringUtils.isBlank(salt)) {
			if (!appCapability.getAppKey().equals(authReq.getAppKey())) {
				logger.error("auth 失败，应用 {0} 的key {1} 错误", authReq.getAppId(),
						authReq.getAppKey());
				return new AuthResp(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR,
						ErrorCode
								.getEmsg(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR));
			}
		} else {
			if (!new MD5(salt + appCapability.getAppKey()).asHex().equals(
					authReq.getAppKey())) {
				logger.error("auth 失败，应用 {0} 的key {1} 错误，salt {2}",
						authReq.getAppId(), authReq.getAppKey(), salt);
				return new AuthResp(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR,
						ErrorCode
								.getEmsg(ErrorCode.ECODE_ACCOUNT_OR_PASSWD_ERR));
			}
		}
		logger.info("auth 成功，appId {0}, appKey {1}，scope {2}，salt {3}",
				authReq.getAppId(), authReq.getAppKey(), authReq.getScope(),
				salt);
		return new AuthResp(ErrorCode.ECODE_SUCCESS,
				ErrorCode.getEmsg(ErrorCode.ECODE_SUCCESS));
	}

	@Override
	public boolean accessRateLimit(String appId, String capabilityId, String api) {
		// TODO Auto-generated method stub
		AppCapability appCapability = findAppCapability(appId, capabilityId);
		if (!appCapability.isCheckAccessRate()) {
			return false;
		}
		String key = "profile:app.accessrate." + appId + "." + api;
		Integer rate = (Integer) localCache.getFromCache(key);
		if (rate == null) {
			rate = readAccessRateFromDb(appId, api);
			if (rate != null) {
				localCache.putInCache(key, rate);
			} else {
				rate = 0;
			}
		}
		if (rate < 1) {
			return true;
		}
		return AccessLimit.incAndCheckAccessCount(appId, api, rate);
	}

	@Override
	public boolean authorization(String appId, String capabilityId, String api) {
		// TODO Auto-generated method stub
		AppCapability appCapability = findAppCapability(appId, capabilityId);
		if (!appCapability.isCheckAccessPrivilege()) {
			return true;
		}
		String key = "profile:app.authorization." + appId + "." + api;
		Boolean canAccess = (Boolean) localCache.getFromCache(key);
		if (canAccess == null) {
			canAccess = authorizationFromDb(appId, api);
			localCache.putInCache(key, canAccess);
		}
		return canAccess;
	}

	private AppCapability authFromDb(String appId, String capabilityId) {
		// TODO Auto-generated method stub
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String select = "select a.APP_KEY,a.VALID_TYPE,a.LOCK_FLAG"
				+ ",c.STATUS,c.VALID_TIME,c.DEADLINE_TIME,a.IP,a.SHARE_KEY "
				+ "from TI_APP_INFO a, TI_USER_AUTHORIZATION b, TI_USER_CAPABILITY c "
				+ "where a.APP_ID=b.APP_ID and a.USER_ID=b.USER_ID and b.CAPABILITY_ID=? "
				+ "and a.APP_ID=? and c.USER_ID=b.USER_ID and c.CAPABILITY_ID=b.CAPABILITY_ID "
				+ "and c.USER_CAPABILITY_ID=b.USER_CAPABILITY_ID";
		try {
			conn = getConnection();
			stmt = conn.prepareStatement(select);
			stmt.setString(1, capabilityId);
			stmt.setString(2, appId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				Date validTime = rs.getDate(5);
				Date deadlineTime = rs.getDate(6);
				if (validTime != null
						&& validTime.getTime() > System.currentTimeMillis()) {
					// 未生效
					logger.error("应用 {0} 授权的能力 {1} 未生效，生效时间 {2}", appId,
							capabilityId, validTime);
					return null;
				}
				if (deadlineTime != null
						&& deadlineTime.getTime() < System.currentTimeMillis()) {
					// 已失效
					logger.error("应用 {0} 授权的能力 {1} 已失效，失效时间 {2}", appId,
							capabilityId, validTime);
					return null;
				}
				AppCapability appCapability = new AppCapability();
				appCapability.setAppId(appId);
				appCapability.setAppKey(rs.getString(1));
				appCapability.setValid(rs.getInt(2) == 1 ? true : false);
				appCapability.setLocked(rs.getInt(3) == 0 ? false : true);
				appCapability.setCapability(CAPABILITY.valueOf(capabilityId));
				appCapability.setStatus(USER_STATUS.valueOf(rs.getInt(4)));
				appCapability.setServiceURL("");
				appCapability.setIpList(rs.getString(7));
				appCapability.setSharedKey(rs.getString(8));
				return appCapability;
			}
			return null;
		} catch (Exception e) {
			logger.error("查询应用能力授权信息失败", e);
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
		return null;
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

	private Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		return dataSource.getConnection();
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public Cache getLocalCache() {
		return localCache;
	}

	public void setLocalCache(Cache localCache) {
		this.localCache = localCache;
	}

	public Redis getSsoRedis() {
		return ssoRedis;
	}

	public void setSsoRedis(Redis ssoRedis) {
		this.ssoRedis = ssoRedis;
	}

	static class AppCapability {
		private String appId;
		private String appKey;
		private String sharedKey;
		private boolean valid = true;// 应用是否有效
		private boolean isLocked = false;// 应用是否已被锁定

		private CAPABILITY capability;
		private String serviceURL;
		private USER_STATUS status = USER_STATUS.NOMARL;// 用户状态
		private String ipList;// 应用侧ip地址列表，多个地址之间用英文逗号分隔

		private boolean checkAccessPrivilege = false; // 检查应用是否允许访问能力api
		private boolean checkAccessRate = false; // 检查应用访问api频率

		private boolean isNull = false;// 空的应用能力信息

		public String getAppId() {
			return appId;
		}

		public static AppCapability instanceNull() {
			// TODO Auto-generated method stub
			AppCapability nullAppCapability = new AppCapability();
			nullAppCapability.isNull = true;
			return nullAppCapability;
		}

		public void setAppId(String appId) {
			this.appId = appId;
		}

		public String getAppKey() {
			return appKey;
		}

		public void setAppKey(String appKey) {
			this.appKey = appKey;
		}

		public CAPABILITY getCapability() {
			return capability;
		}

		public void setCapability(CAPABILITY capability) {
			this.capability = capability;
		}

		public String getServiceURL() {
			return serviceURL;
		}

		public void setServiceURL(String serviceURL) {
			this.serviceURL = serviceURL;
		}

		public boolean isValid() {
			return valid;
		}

		public void setValid(boolean valid) {
			this.valid = valid;
		}

		public USER_STATUS getStatus() {
			return status;
		}

		public void setStatus(USER_STATUS status) {
			this.status = status;
		}

		public boolean isNull() {
			return isNull;
		}

		public boolean isLocked() {
			return isLocked;
		}

		public void setLocked(boolean isLocked) {
			this.isLocked = isLocked;
		}

		public String getIpList() {
			return ipList;
		}

		public void setIpList(String ipList) {
			this.ipList = ipList;
		}

		public String getSharedKey() {
			return sharedKey;
		}

		public void setSharedKey(String sharedKey) {
			this.sharedKey = sharedKey;
		}

		public boolean isCheckAccessPrivilege() {
			return checkAccessPrivilege;
		}

		public void setCheckAccessPrivilege(boolean checkAccessPrivilege) {
			this.checkAccessPrivilege = checkAccessPrivilege;
		}

		public boolean isCheckAccessRate() {
			return checkAccessRate;
		}

		public void setCheckAccessRate(boolean checkAccessRate) {
			this.checkAccessRate = checkAccessRate;
		}

	}

	private Integer readAccessRateFromDb(String appId, String api) {
		// TODO Auto-generated method stub
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String select = "select RATE from TI_APP_ACCESS_RATE "
				+ "where APP_ID=? and API=?";
		try {
			conn = getConnection();
			stmt = conn.prepareStatement(select);
			stmt.setString(1, appId);
			stmt.setString(1, api);
			rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		} catch (Exception e) {
			logger.error(
					"查询应用访问接口频率限制[TI_APP_ACCESS_RATE]失败:" + e.getMessage(), e);
			return null;
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

	private Boolean authorizationFromDb(String appId, String api) {
		String key = "profile:app.authorizations." + appId;
		List<String> authorizations = (List<String>) localCache
				.getFromCache(key);
		if (authorizations == null) {
			logger.info("从表TI_APP_ACCESS_PRIVILEGE读取应用 {0} api访问列表", appId);
			authorizations = findAuthorizationsFromDb(appId);
			if (authorizations != null) {
				getLocalCache().putInCache(key, authorizations);
			} else {
				authorizations = new ArrayList<String>();
				getLocalCache().putInCache(key, authorizations);
			}
		}
		for (String authorization : authorizations) {
			int matched = VerifyHelper.isMatch(authorization, api);
			if (matched == 1) {
				return true;
			}
		}
		return false;
	}

	private List<String> findAuthorizationsFromDb(String appId) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String select = "select API from TI_APP_ACCESS_PRIVILEGE "
				+ "where APP_ID=?";
		List<String> authorizations = new ArrayList<String>();
		try {
			conn = getConnection();
			stmt = conn.prepareStatement(select);
			stmt.setString(1, appId);
			rs = stmt.executeQuery();
			while (rs.next()) {
				authorizations.add("^" + toPatern(rs.getString(1)) + "$");
			}
			return authorizations;
		} catch (Exception e) {
			logger.error(
					"查询应用访问接口权限[TI_APP_ACCESS_PRIVILEGE]失败:" + e.getMessage(),
					e);
			return authorizations;
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

	private AppCapability findAppCapability(String appId, String capabilityId) {
		String key = "profile:app.capability." + appId + "." + capabilityId;
		AppCapability appCapability = (AppCapability) localCache
				.getFromCache(key);
		if (appCapability == null) {
			appCapability = authFromDb(appId, capabilityId);
			if (appCapability == null) {
				appCapability = AppCapability.instanceNull();
			}
			localCache.putInCache(key, appCapability);
		}
		return appCapability;
	}

	/**
	 * 20160726 wangtw 修改.*为[\S\s]*，.?为[\S\s]?，解决.*或.?无法匹配\n（回车符）的bug
	 * 
	 * @param s
	 * @return
	 */
	private String toPatern(String s) {
		s = s.replace('.', '#');
		s = s.replaceAll("#", "\\\\.");
		s = s.replace('*', '#');
		// s = s.replaceAll("#", ".*");
		s = s.replaceAll("#", "[\\\\S\\\\s]*");
		s = s.replace('?', '#');
		// s = s.replaceAll("#", ".?");
		s = s.replaceAll("#", "[\\\\S\\\\s]?");
		return s;
	}

}
