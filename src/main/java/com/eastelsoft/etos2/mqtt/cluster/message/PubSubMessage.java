package com.eastelsoft.etos2.mqtt.cluster.message;

import java.io.Serializable;

public abstract class PubSubMessage implements Serializable {

    private static final long serialVersionUID = -8789343104393884987L;

    private Long nodeId;

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

}
