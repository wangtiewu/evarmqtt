package com.eastelsoft.etos2.mqtt.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * 类说明：jackson工具类
 */
public class JacksonUtils {
	private static final Logger logger = LoggerFactory
			.getLogger(JacksonUtils.class);
	private static String[] formats = new String[] { "yyyyMMddHHmmss",
			"yyyyMMdd" };
	private static ObjectMapper mapper = new ObjectMapper();
	static {
		// 忽略在JSON字符串中存在但Java对象实际没有的属�?
		// mapper.getSerializationConfig().
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
	}

	public static <T> String bean2Json(T bean) {
		try {
			return mapper.writeValueAsString(bean);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static String map2Json(Map map) {
		try {
			return mapper.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static String list2Json(List list) {
		try {
			return mapper.writeValueAsString(list);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	public static String ary2Json(Object[] ary) {
		try {
			return mapper.writeValueAsString(ary);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static <T> T json2Bean(String json, Class<T> beanClass) {
		try {
			return mapper.readValue(json, beanClass);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static <T> List<T> json2List(String json, Class<T> beanClass) {
		try {
			return (List<T>) mapper.readValue(json,
					getCollectionType(List.class, beanClass));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static Map json2Map(String json) {
		try {
			return (Map) mapper.readValue(json, Map.class);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	public static <T> T[] json2Ary(String json, Class<T> beanClass) {
		try {
			return listToArray(json2List(json, beanClass), beanClass);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	
	private static <T> T[] listToArray(List<T> src, Class<T> type) {
		if (src == null || src.isEmpty()) {
			return null;
		}
		// 初始化泛型数组
		// JAVA中不允许这样初始化泛型数组： T[] dest = new T[src.size()];
		T[] dest = (T[]) Array.newInstance(type, src.size());
		for (int i = 0; i < src.size(); i++) {
			dest[i] = src.get(i);
		}
		return (T[]) dest;
	}
	
	private static JavaType getCollectionType(Class<?> collectionClass,
			Class<?>... elementClasses) {
		return mapper.getTypeFactory().constructParametricType(collectionClass,
				elementClasses);
	}

	public static void main(String[] args) {
		String str = "Hello World";
		String json = JacksonUtils.bean2Json(str);
		System.out.println(json);
		str = JacksonUtils.json2Bean(json, String.class);
		System.out.println(str);
	}

}
