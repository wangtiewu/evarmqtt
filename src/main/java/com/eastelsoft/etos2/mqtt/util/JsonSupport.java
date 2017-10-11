package com.eastelsoft.etos2.mqtt.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * JSON infrastructure interface.
 * Allows to implement custom realizations
 * to JSON support operations.
 *
 */
public interface JsonSupport {

    void writeJsonpValue(OutputStream out, Object value) throws IOException;

    <T> T readValue(String namespaceName, InputStream src, Class<T> valueType) throws IOException;

    void writeValue(OutputStream out, Object value) throws IOException;

    void addEventMapping(String namespaceName, String eventName, Class<?> ... eventClass);

    void removeEventMapping(String namespaceName, String eventName);

    List<byte[]> getArrays();

}
