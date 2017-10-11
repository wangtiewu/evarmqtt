package com.eastelsoft.etos2.mqtt.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class ReflectionUtils {
	private static final Logger logger = LoggerFactory
			.getLogger(ReflectionUtils.class);
	private static Map<Class, Map<String, Field>> fieldMap = new HashMap<Class, Map<String, Field>>();

	/**
	 * 获取对象的指定字段，Field.setAccessible(true);
	 * 
	 * @param obj
	 * @param fieldName
	 * @return
	 */
	public static Field getField(Object obj, String fieldName) {
		return getField(obj.getClass(), fieldName);
	}

	/**
	 * 获取类的指定字段，Field.setAccessible(true);
	 * 
	 * @param cls
	 * @param fieldName
	 * @return
	 */
	public static Field getField(Class cls, String fieldName) {
		Field f = null;
		Map<String, Field> inner = fieldMap.get(cls);
		if (inner != null) {
			f = inner.get(fieldName);
			if (f != null) {
				return f;
			}
			else {
				if (inner.containsKey(fieldName)) {
					return inner.get(fieldName);
				}
				else {
					//need do find
				}
			}
		} else {
			inner = new HashMap<String, Field>();
			Map<String, Field> oldInner = fieldMap.putIfAbsent(cls, inner);
			if (oldInner != null) {
				inner = oldInner;
				f = inner.get(fieldName);
				if (f != null) {
					return f;
				}
				else {
					if (inner.containsKey(fieldName)) {
						return inner.get(fieldName);
					}
					else {
						//need do find
					}
				}
			}
		}
		try {
			while (cls != null) {
				try {
					f = cls.getDeclaredField(fieldName);
					f.setAccessible(true);
				} catch (NoSuchFieldException e) {
					logger.error("类 " + cls +"上字段 " + fieldName +"不存在，从父类查找："+e.getMessage(), e);
					f = null;
				}
				if (f != null)
					break;
				cls = cls.getSuperclass();
			}
		} catch (SecurityException e) {
			logger.error("类 " + cls +"上字段 " + fieldName +"不存在，从父类查找："+e.getMessage(), e);
		}
		if (f == null) {
			logger.error("类 " + cls +"上无方法 " + fieldName);
		}
		inner.put(fieldName, f);
		return f;
	}

	/**
	 * 获取对象指定字段的值
	 * 
	 * @param obj
	 * @param field
	 * @return
	 */
	public static Object getFieldValue(Object obj, Field field) {
		try {
			return field.get(obj);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 设置对象指定字段的值
	 * 
	 * @param obj
	 * @param field
	 * @param value
	 */
	public static void setFieldValue(Object obj, Field field, Object value) {
		field.setAccessible(true);
		try {
			field.set(obj, value);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("IllegalArgumentException", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("IllegalAccessExceptione", e);
		}
	}

	/**
	 * 获取类的所有字段（包括父类）
	 * 
	 * @param clz
	 * @return
	 */
	public static Field[] getAllFields(Class clz) {
		ArrayList<Field> fieldList = new ArrayList<Field>();
		getAllFieldsRecursive(clz, fieldList);
		if (fieldList.isEmpty())
			return new Field[0];
		else
			return fieldList.toArray(new Field[0]);
	}

	/**
	 * 获取类的所有非静态字段（包括父类）
	 * 
	 * @param clz
	 * @return
	 */
	public static Field[] getAllNonStaticFields(Class clz) {
		List<Field> fList = new ArrayList<Field>();
		getAllFieldsRecursive(clz, fList);
		List<Field> nonStaticFields = new LinkedList<Field>();
		for (Field f : fList) {
			if ((f.getModifiers() & Modifier.STATIC) == 0) {
				nonStaticFields.add(f);
			}
		}
		if (nonStaticFields.isEmpty())
			return new Field[0];
		else
			return nonStaticFields.toArray(new Field[0]);
	}

	/**
	 * 获取类的所有非静态字段，并把字段的Accessible设置为true
	 * 
	 * @param clz
	 * @return
	 */
	public static Field[] getDeclaredNonStaticFields(Class clz) {
		List<Field> nonStaticFields = new LinkedList<Field>();
		for (Field f : clz.getDeclaredFields()) {
			if (!Modifier.isStatic(f.getModifiers())) {
				f.setAccessible(true);
				nonStaticFields.add(f);
			}
		}
		if (nonStaticFields.isEmpty())
			return new Field[0];
		else
			return nonStaticFields.toArray(new Field[0]);
	}

	/**
	 * 获取类及所有父类的方法，如果找不到则抛出异常NoSuchMethodException
	 * 
	 * @param clz
	 *            - declaring class of the method
	 * @param methodName
	 *            - name of the method
	 * @param paramTypes
	 *            - method paramTypes
	 * @return requested method
	 * @throws NoSuchMethodException
	 */
	public static Method getMethod(Class<? extends Object> clz,
			String methodName, Class[] paramTypes) throws NoSuchMethodException {
		if (clz == null)
			throw new NoSuchMethodException(methodName + "(" + paramTypes
					+ ") method does not exist ");
		try {
			return clz.getDeclaredMethod(methodName, paramTypes);
		} catch (NoSuchMethodException e) {
			return getMethod(clz.getSuperclass(), methodName, paramTypes);
		}
	}

	/**
	 * <p>
	 * Invokes method of a given obj and set of parameters
	 * </p>
	 * 
	 * @param obj
	 *            - Object for which the method is going to be executed. Special
	 *            if obj is of type java.lang.Class, method is executed as
	 *            static method of class given as obj parameter
	 * @param method
	 *            - name of the object
	 * @param params
	 *            - actual parameters for the method
	 * @return - result of the method execution
	 * 
	 *         <p>
	 *         If there is any problem with method invocation, a
	 *         RuntimeException is thrown
	 *         </p>
	 */
	public static Object invoke(Object obj, String method, Class[] params,
			Object[] args) {
		try {
			Class<?> clazz = null;
			if (obj instanceof Class) {
				clazz = (Class) obj;
			} else {
				clazz = obj.getClass();
			}
			Method methods = clazz.getMethod(method, params);
			if (obj instanceof Class) {
				return methods.invoke(null, args);
			} else {
				return methods.invoke(obj, args);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 是否基础类型、或基础类型包装类、或String类
	 * 
	 * @param clz
	 * @return
	 */
	public static boolean isPrimitiveOrString(Class<? extends Object> clz) {
		// TODO Auto-generated method stub
		if (clz.equals(String.class)) {
			return true;
		}
		if (clz.isPrimitive()) {
			return true;
		}
		// 判断是否是Wrap
		return clz.equals(Boolean.class) || clz.equals(Integer.class)
				|| clz.equals(Character.class) || clz.equals(Byte.class)
				|| clz.equals(Short.class) || clz.equals(Double.class)
				|| clz.equals(Long.class) || clz.equals(Float.class);
	}

	/**
	 * 是否自定义类
	 * 
	 * @param clz
	 * @return
	 */
	public static boolean isCustomizedModel(Class<? extends Object> clz) {
		// TODO Auto-generated method stub
		return clz != null && clz.getClassLoader() != null;
	}
	
	public static Object convertParam(String param, Class<?> paramType,
			Type genericType) {
		// TODO Auto-generated method stub
		if (paramType == String.class) {
			return param;
		} else if (paramType == boolean.class || paramType == Boolean.class) {
			return Boolean.parseBoolean(param);
		} else if (paramType == byte.class || paramType == Byte.class) {
			return Byte.parseByte(param);
		} else if (paramType == char.class || paramType == Character.class) {
			return param.charAt(0);
		} else if (paramType == short.class || paramType == Short.class) {
			return Short.valueOf(param);
		} else if (paramType == int.class || paramType == Integer.class) {
			return Integer.parseInt(param);
		} else if (paramType == long.class || paramType == Long.class) {
			return Long.parseLong(param);
		} else if (paramType == double.class || paramType == Double.class) {
			return Double.parseDouble(param);
		} else if (paramType == float.class || paramType == Float.class) {
			return Float.parseFloat(param);
		} else if (paramType == List.class) {
			// List
			if (genericType instanceof ParameterizedType) {
				ParameterizedType parameterizedType = (ParameterizedType) genericType;
				// 返回表示此类型实际类型参数的 Type 对象的数组
				Type[] actualTypeArguments = parameterizedType
						.getActualTypeArguments();
				return JacksonUtils.json2List(param,
						(Class<?>) actualTypeArguments[0]);
			}
		} else if (paramType == Map.class) {
			return JacksonUtils.json2Map(param);
		} else if (paramType.isArray()) {
			return JacksonUtils.json2Ary(param, paramType.getComponentType());
		}
		return param;
	}

	private static void getAllFieldsRecursive(Class clz, List<Field> fldLst) {
		if (clz.isPrimitive())
			return;
		for (Field f : clz.getDeclaredFields()) {
			f.setAccessible(true);
			fldLst.add(f);
		}
		if (clz.getSuperclass() == Object.class)
			return;
		else
			getAllFieldsRecursive(clz.getSuperclass(), fldLst);
	}
}
