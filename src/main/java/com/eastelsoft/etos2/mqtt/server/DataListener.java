package com.eastelsoft.etos2.mqtt.server;

import java.util.List;
import java.util.Map;


public interface DataListener<T> {

    /**
     * Invokes when data object received from client
     *
     * @param client - receiver
     * @param parameters - recived parameters，指http 请求地址中的参数
     * @param data - received object，指http body
     */
    void onData(Client client, Map<String, List<String>> parameters, T data) throws Exception;

}
