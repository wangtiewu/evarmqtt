package com.eastelsoft.etos2.mqtt.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * ip white list loader
 * 
 * @author wangzl
 * @version 2016-05-11
 */
public class IpListLoader {

	private static IpListLoader loader = new IpListLoader();

	private static List<String> ipList = null;

	public static IpListLoader getInstance() {
		return loader;
	}

	public List<String> getIpList() {
		if (ipList == null) {
			synchronized (IpListLoader.class) {
				if (ipList == null) {
					ipList = new ArrayList<String>();
					loadList();
				}
			}
		}
		return ipList;
	}

	private static void loadList() {
		InputStream is = null;
		try {
			is = IpListLoader.class.getClassLoader().getResourceAsStream(
					"ipwhitelist.txt");
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));

			String line = null;
			while ((line = reader.readLine()) != null) {
				ipList.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
