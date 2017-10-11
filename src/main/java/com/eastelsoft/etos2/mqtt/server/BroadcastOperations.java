package com.eastelsoft.etos2.mqtt.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.eastelsoft.etos2.mqtt.cluster.PubSubType;
import com.eastelsoft.etos2.mqtt.cluster.StoreFactory;
import com.eastelsoft.etos2.mqtt.cluster.message.DispatchMessage;

/**
 * Fully thread-safe.
 *
 */
public class BroadcastOperations {

	private final Iterable<Client> clients;
	private final Map<String, List<String>> namespaceRooms = new HashMap<String, List<String>>();
	private final StoreFactory storeFactory;

	public BroadcastOperations(Namespace namespace, String room,
			StoreFactory storeFactory) {
		super();
		clients = namespace.getRoomClients(room);
		List<String> roomsList = new ArrayList<String>();
		roomsList.add(room);
		namespaceRooms.put(namespace.getName(), roomsList);
		this.storeFactory = storeFactory;
	}

	private void dispatch(BaseMessage message) {
		for (Entry<String, List<String>> entry : namespaceRooms.entrySet()) {
			for (String room : entry.getValue()) {
				storeFactory.pubSubStore().publish(PubSubType.DISPATCH,
						new DispatchMessage(room, message, entry.getKey()));
			}
		}
	}

	public void sendEvent(String eventName, BaseMessage message) {
		if (clients != null) {
			for (Client client : clients) {
				client.send(eventName, message);
			}
		}
		dispatch(message);
	}

}
