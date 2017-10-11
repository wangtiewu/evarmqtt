package com.eastelsoft.etos2.mqtt.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.junit.Test;
import org.nlpcn.commons.lang.tire.domain.Forest;
import org.nlpcn.commons.lang.tire.library.Library;

import com.eastelsoft.etos2.mqtt.server.Topic;

import eet.evar.tool.trie.TrieUtil;

public class DoubleArrayTrieTest {

	@Test
	public void test() throws Exception {
		List<String> words = new ArrayList<String>();
		words.add("法轮功");
		words.add("操你妈");
		words.add("日内瓦金融");
		words.add("沐足");
		words.add("猪毛");
		words.add("奖 券");
		words.add("发漂");
		words.add("限制");
		words.add("民警");
		words.add("保钓");
		words.add("基督");
		words.add("贷款");
		words.add("私募");
		words.add("包赢");
		words.add("必中");
		words.add("代办");
		words.add("评估车辆");
		words.add("讲法");
		words.add("修炼");
		words.add("汇款");
		words.add("楼面");
		words.add("账户");
		words.add("账号");
		words.add("跨网");
		words.add("地价");
		words.add("住宅");
		words.add("发票");
		words.add("税票");
		words.add("小灵通");
		words.add("付款");
		words.add("出卖");
		words.add("房产");
		words.add("清真");
		words.add("公开信");
		words.add("发票");
		words.add("税票");
		words.add("购房");
		words.add("小米");
		words.add("付款");
		words.add("中 央");
		words.add("认筹");
		words.add("直销");
		words.add("中奖");
		words.add("一举");
		words.add("一举一动");
		words.add("一举成名");
		words.add("一举成名天下知");
		words.add("万能");
		words.add("万能胶");
		words.add("一举");
		words.add("test");
		words.add("+/");
		Collections.sort(words);
		DoubleArrayTrie darts = new DoubleArrayTrie();
		darts.build(words);
		System.out.println(darts.exactMatchSearch("test"));
		System.out.println(darts.exactMatchSearch("+/"));
		System.out.println(darts.exactMatchSearch("一举成名天下知"));
		System.out.println(darts.exactMatchSearch("world"));

		final int count = 400000;
		List<String> topicFilters = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			topicFilters.add(("abc" + i + "/" + i + "/test"));
		}
		Collections.sort(topicFilters);
		long t = System.currentTimeMillis();
		darts.build(topicFilters);
		System.out
				.println("insert qos="
						+ (count / Math.max(1,
								(System.currentTimeMillis() - t) / 1000)));
		t = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			assertTrue(darts.exactMatchSearch(("abc" + i + "/" + i + "/test")) > -1);
		}
		System.out
				.println("match qos="
						+ (count / Math.max(1,
								(System.currentTimeMillis() - t) / 1000)));
		darts.clear();

		// 测试ansj的tire
		Forest forest = new Forest();
		t = System.currentTimeMillis();
		for (String w : topicFilters) {
			Library.insertWord(forest, w);
		}
		System.out
		.println("insert qos="
				+ (count / Math.max(1,
						(System.currentTimeMillis() - t) / 1000)));

		t = System.currentTimeMillis();
		for (int i = 0; i < count; ++i) {
			if (forest.getWord(("abc" + i + "/" + i + "/test")).getAllWords().equals(("abc" + i + "/" + i + "/test")) == false) {
				throw new RuntimeException("ansj没找到该有的词");
			}
		}
		System.out
		.println("match qos="
				+ (count / Math.max(1,
						(System.currentTimeMillis() - t) / 1000)));
	}

}
