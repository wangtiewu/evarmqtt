package com.eastelsoft.etos2.mqtt.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.eastelsoft.etos2.mqtt.server.BaseRespMessage;
import com.thoughtworks.xstream.XStream;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class XStreamXmlSupport implements XmlSupport {
	private static final Logger logger = LoggerFactory
			.getLogger(XStreamXmlSupport.class);
	private Map<Class<?>, String> aliasMap;
	private Map<Class<?>, XStream> xstreamMap;

	private static class XStreamXmlSupportHolder {
		private static XStreamXmlSupport singleInstance = new XStreamXmlSupport();
	}

	public static XStreamXmlSupport getInstance() {
		return XStreamXmlSupportHolder.singleInstance;
	}

	private XStreamXmlSupport() {
		aliasMap = new HashMap<Class<?>, String>();
		xstreamMap = new HashMap<Class<?>, XStream>();
		addAliasMapping("Response", BaseRespMessage.class);
	}

	@Override
	public <T> T readValue(InputStream src, Class<T> valueType)
			throws IOException {
		// TODO Auto-generated method stub
		XStream xs = xstreamMap.get(valueType);
		if (xs == null) {
			xs = newXStream(valueType, valueType.getSimpleName());
		}
		T value = (T) xs.fromXML(src);
		return value;
	}

	@Override
	public void writeValue(OutputStream out, Object value) throws IOException {
		XStream xs = xstreamMap.get(value.getClass());
		if (xs == null) {
			xs = newXStream(value.getClass(), value.getClass().getSimpleName());
		}
		xs.toXML(value, out);
	}

	@Override
	public String bean2Xml(Object value, String alias) {
		// TODO Auto-generated method stub
		XStream xs = xstreamMap.get(value.getClass());
		if (xs == null) {
			if (StringUtils.isEmpty(alias)) {
				alias = value.getClass().getSimpleName();
			}
			xs = newXStream(value.getClass(), alias);
		}
		return xs.toXML(value);
	}

	@Override
	public void addAliasMapping(String alias, Class<?>... clzs) {
		// TODO Auto-generated method stub
		for (Class<?> clz : clzs) {
			if (aliasMap.containsKey(clz)) {
				logger.warn("类 {0} 的别名 {1} 设置已存在，将被替换为 {2}", clz,
						aliasMap.get(clzs), alias);
			}
			aliasMap.putIfAbsent(clz, alias);
			newXStream(clz, alias);
			logger.info("类 {0} 的别名设置为 {1}", clz, alias);
		}
	}

	@Override
	public void removeAliasMapping(Class<?>... clzs) {
		// TODO Auto-generated method stub
		for (Class<?> clz : clzs) {
			aliasMap.remove(clz);
			xstreamMap.remove(clz);
			logger.info("类 {0} 的别名已移除", clz);
		}
	}

	private XStream newXStream(Class<?> clz, String alias) {
		XStream xs = new XStream();
		xs.setMode(XStream.NO_REFERENCES);
		xs.autodetectAnnotations(true);
		xs.alias(alias, clz);
		XStream oldXs = xstreamMap.putIfAbsent(clz, xs);
		if (oldXs != null) {
			xs = oldXs;
		}
		return xs;
	}

}
