/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.eastelsoft.etos2.mqtt.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NamespacesHub {
	private final ConcurrentMap<String, Namespace> namespaces = new ConcurrentHashMap<String, Namespace>();
	private Configuration configuration;
	private MqttServer mqttServer;

	public NamespacesHub(MqttServer mqttServer, Configuration configuration) {
		this.configuration = configuration;
		this.mqttServer = mqttServer;
	}

	public Namespace create(String name) {
		Namespace namespace = (Namespace) namespaces.get(name);
		if (namespace == null) {
			namespace = new NamespaceImpl(name, configuration, mqttServer);
			Namespace oldNamespace = (Namespace) namespaces.putIfAbsent(name,
					namespace);
			if (oldNamespace != null) {
				namespace = oldNamespace;
			}
		}
		return namespace;
	}

	public Namespace create(String name, int ipAccLimCount) {
		Namespace namespace = (Namespace) namespaces.get(name);
		if (namespace == null) {
			namespace = new NamespaceImpl(name, ipAccLimCount,
					configuration, mqttServer);
			Namespace oldNamespace = (Namespace) namespaces.putIfAbsent(name,
					namespace);
			if (oldNamespace != null) {
				namespace = oldNamespace;
			}
		}
		return namespace;
	}

	public Iterable<Client> getRoomClients(String room) {
		List<Iterable<Client>> allClients = new ArrayList<Iterable<Client>>();
		for (Namespace namespace : namespaces.values()) {
			Iterable<Client> clients = ((Namespace) namespace)
					.getRoomClients(room);
			allClients.add(clients);
		}
		return new CompositeIterable<Client>(allClients);
	}

	public Namespace get(String name) {
		if (name == null || name.equals("")) {
			return null;
		}
		return (Namespace) namespaces.get(name);
	}

	public void remove(String name) {
		namespaces.remove(name);
	}

	public Collection<Namespace> getAllNamespaces() {
		return namespaces.values();
	}

}
