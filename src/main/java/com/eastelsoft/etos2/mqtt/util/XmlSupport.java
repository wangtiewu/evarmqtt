package com.eastelsoft.etos2.mqtt.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface XmlSupport {

    <T> T readValue(InputStream src, Class<T> valueType) throws IOException;
    
    void writeValue(OutputStream out, Object value) throws IOException;

    String bean2Xml(Object value, String alias);

    void addAliasMapping(String alias, Class<?> ... clzs);

    void removeAliasMapping(Class<?> ... clzs);

}
