package com.eastelsoft.etos2.mqtt.server;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventEntry<T> {
	private final Class<T> eventClass;

    private final Queue<DataListener<T>> listeners = new ConcurrentLinkedQueue<DataListener<T>>();;

    public EventEntry(Class<T> eventClass) {
        super();
        this.eventClass = eventClass;
    }
    

    public Class<T> getEventClass() {
		return eventClass;
	}



	public void addListener(DataListener<T> listener) {
        listeners.add(listener);
    }

    public Queue<DataListener<T>> getListeners() {
        return listeners;
    }

}
