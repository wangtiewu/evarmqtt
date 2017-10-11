package com.eastelsoft.etos2.mqtt.monitor;

public class MonitorFactory {
	private static Monitor monitor = null;

	public static Monitor getInstance() {
		if (monitor == null) {
			synchronized (MonitorFactory.class) {
				if (monitor == null) {
					monitor = new Monitor();
				}
			}
		}
		return monitor;
	}
	
	public static void shutdown() {
		if (monitor != null) {
			monitor.stop();
			monitor = null;
		}
	}
}
