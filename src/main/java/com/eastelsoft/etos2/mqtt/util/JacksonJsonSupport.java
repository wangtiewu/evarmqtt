package com.eastelsoft.etos2.mqtt.util;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

import com.eastelsoft.etos2.mqtt.server.Namespace;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.ArrayType;

public class JacksonJsonSupport implements JsonSupport {

	public static class HTMLCharacterEscapes extends CharacterEscapes {
		private static final long serialVersionUID = 6159367941232231133L;

		private final int[] asciiEscapes;

		public HTMLCharacterEscapes() {
			// start with set of characters known to require escaping
			// (double-quote, backslash etc)
			int[] esc = CharacterEscapes.standardAsciiEscapesForJSON();
			// and force escaping of a few others:
			esc['"'] = CharacterEscapes.ESCAPE_CUSTOM;
			asciiEscapes = esc;
		}

		// this method gets called for character codes 0 - 127
		@Override
		public int[] getEscapeCodesForAscii() {
			return asciiEscapes;
		}

		@Override
		public SerializableString getEscapeSequence(int ch) {
			if (ch == '"') {
				return new SerializedString("\\" + (char) ch);
			}
			return null;
		}
	}

	public static class EventKey {

		private String namespaceName;
		private String eventName;

		public EventKey(String namespaceName, String eventName) {
			super();
			this.namespaceName = namespaceName;
			this.eventName = eventName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((eventName == null) ? 0 : eventName.hashCode());
			result = prime * result
					+ ((namespaceName == null) ? 0 : namespaceName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EventKey other = (EventKey) obj;
			if (eventName == null) {
				if (other.eventName != null)
					return false;
			} else if (!eventName.equals(other.eventName))
				return false;
			if (namespaceName == null) {
				if (other.namespaceName != null)
					return false;
			} else if (!namespaceName.equals(other.namespaceName))
				return false;
			return true;
		}

	}

	private class EventDeserializer extends StdDeserializer<Event> {

		private static final long serialVersionUID = 8178797221017768689L;

		final Map<EventKey, List<Class<?>>> eventMapping = new ConcurrentHashMap<EventKey, List<Class<?>>>();

		protected EventDeserializer() {
			super(Event.class);
		}

		@Override
		public Event deserialize(JsonParser jp, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			ObjectMapper mapper = (ObjectMapper) jp.getCodec();
			String eventName = jp.nextTextValue();

			EventKey ek = new EventKey(namespaceClass.get(), eventName);
			if (!eventMapping.containsKey(ek)) {
				ek = new EventKey(Namespace.DEFAULT_NAME, eventName);
				if (!eventMapping.containsKey(ek)) {
					return new Event(eventName, Collections.emptyList());
				}
			}

			List<Object> eventArgs = new ArrayList<Object>();
			Event event = new Event(eventName, eventArgs);
			List<Class<?>> eventClasses = eventMapping.get(ek);
			int i = 0;
			while (true) {
				JsonToken token = jp.nextToken();
				if (token == JsonToken.END_ARRAY) {
					break;
				}
				if (i > eventClasses.size() - 1) {
					if (log.isDebugEnabled()) {
						log.debug(
								"Event {} has more args than declared in handler: {}",
								eventName, null);
					}
					break;
				}
				Class<?> eventClass = eventClasses.get(i);
				Object arg = mapper.readValue(jp, eventClass);
				eventArgs.add(arg);
				i++;
			}
			return event;
		}

	}

	public static class ByteArraySerializer extends StdSerializer<byte[]> {

		private final ThreadLocal<List<byte[]>> arrays = new ThreadLocal<List<byte[]>>() {
			protected List<byte[]> initialValue() {
				return new ArrayList<byte[]>();
			};
		};

		public ByteArraySerializer() {
			super(byte[].class);
		}

		@Override
		public boolean isEmpty(byte[] value) {
			return (value == null) || (value.length == 0);
		}

		@Override
		public void serialize(byte[] value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonGenerationException {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("num", arrays.get().size());
			map.put("_placeholder", true);
			jgen.writeObject(map);
			arrays.get().add(value);
		}

		@Override
		public void serializeWithType(byte[] value, JsonGenerator jgen,
				SerializerProvider provider, TypeSerializer typeSer)
				throws IOException, JsonGenerationException {
			serialize(value, jgen, provider);
		}

		@Override
		public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
			ObjectNode o = createSchemaNode("array", true);
			ObjectNode itemSchema = createSchemaNode("string"); // binary values
																// written as
																// strings?
			return o.set("items", itemSchema);
		}

		@Override
		public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor,
				JavaType typeHint) throws JsonMappingException {
			if (visitor != null) {
				JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
				if (v2 != null) {
					v2.itemsFormat(JsonFormatTypes.STRING);
				}
			}
		}

		public List<byte[]> getArrays() {
			return arrays.get();
		}

		public void clear() {
			arrays.set(new ArrayList<byte[]>());
		}

	}

	private class ExBeanSerializerModifier extends BeanSerializerModifier {

		private final ByteArraySerializer serializer = new ByteArraySerializer();

		@Override
		public JsonSerializer<?> modifyArraySerializer(
				SerializationConfig config, ArrayType valueType,
				BeanDescription beanDesc, JsonSerializer<?> serializer) {
			if (valueType.getRawClass().equals(byte[].class)) {
				return this.serializer;
			}

			return super.modifyArraySerializer(config, valueType, beanDesc,
					serializer);
		}

		public ByteArraySerializer getSerializer() {
			return serializer;
		}

	}

	private final ExBeanSerializerModifier modifier = new ExBeanSerializerModifier();
	private final ThreadLocal<String> namespaceClass = new ThreadLocal<String>();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ObjectMapper jsonpObjectMapper = new ObjectMapper();
	private final EventDeserializer eventDeserializer = new EventDeserializer();

	private final Logger log = LoggerFactory.getLogger(getClass());

	public JacksonJsonSupport(Module... modules) {
		if (modules != null && modules.length > 0) {
			objectMapper.registerModules(modules);
			jsonpObjectMapper.registerModules(modules);
		}
		init(objectMapper);
		init(jsonpObjectMapper);
		jsonpObjectMapper.getFactory().setCharacterEscapes(
				new HTMLCharacterEscapes());
	}

	protected void init(ObjectMapper objectMapper) {
		SimpleModule module = new SimpleModule();
		module.setSerializerModifier(modifier);
		module.addDeserializer(Event.class, eventDeserializer);
		
		objectMapper.registerModule(module);

		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(
				DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN,
				true);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);//wtw
	}

	@Override
	public void addEventMapping(String namespaceName, String eventName,
			Class<?>... eventClass) {
		eventDeserializer.eventMapping.put(new EventKey(namespaceName,
				eventName), Arrays.asList(eventClass));
	}

	@Override
	public void removeEventMapping(String namespaceName, String eventName) {
		eventDeserializer.eventMapping.remove(new EventKey(namespaceName,
				eventName));
	}

	@Override
	public <T> T readValue(String namespaceName, InputStream src,
			Class<T> valueType) throws IOException {
		namespaceClass.set(namespaceName);
		return objectMapper.readValue(src, valueType);
	}

	@Override
	public void writeValue(OutputStream out, Object value)
			throws IOException {
		modifier.getSerializer().clear();
		objectMapper.writeValue(out, value);
	}

	@Override
	public void writeJsonpValue(OutputStream out, Object value)
			throws IOException {
		modifier.getSerializer().clear();
		jsonpObjectMapper.writeValue(out, value);
	}

	@Override
	public List<byte[]> getArrays() {
		return modifier.getSerializer().getArrays();
	}

}
