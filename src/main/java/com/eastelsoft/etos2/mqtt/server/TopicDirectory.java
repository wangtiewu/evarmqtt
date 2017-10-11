package com.eastelsoft.etos2.mqtt.server;

import java.util.Set;

public interface TopicDirectory {
    void add(Topic topic);
    void remove(Topic topic);
    Set<Topic> match(Topic topic);
    void destroy();
}
