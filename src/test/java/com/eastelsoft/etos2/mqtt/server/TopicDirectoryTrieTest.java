package com.eastelsoft.etos2.mqtt.server;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.eastelsoft.etos2.mqtt.server.Topic;
import com.eastelsoft.etos2.mqtt.server.TopicDirectoryTrie;

import eet.evar.tool.Logger;
import static org.assertj.core.api.Assertions.assertThat;
import static com.eastelsoft.etos2.mqtt.server.Topic.asTopic;

public class TopicDirectoryTrieTest {

	private TopicDirectoryTrie sut;

	@Before
	public void setUp() {
		Logger.instance();
		sut = new TopicDirectoryTrie();
	}

	@Test
	public void testPerformance() {
		final int count = 400000;
		long t = System.currentTimeMillis();
		Set<String> topicFilters = new HashSet<>();
		for (int i = 0; i < count; i++) {
			sut.add(Topic.asTopic(("abc/" + i + "/test")));
			// topicFilters.add(("abc" + i +"/" + i + "/test"));
		}
		// sut.init(topicFilters);
		System.out.println("insert qos: " + count
				/ Math.max(1, ((System.currentTimeMillis() - t) / 1000)));

		t = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			assertEquals(1,
					sut.match(Topic.asTopic(("abc/" + i + "/test")))
							.size());
		}
		System.out.println("match qos: " + count
				/ Math.max(1, ((System.currentTimeMillis() - t) / 1000)));

		t = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			sut.remove(Topic.asTopic(("abc/" + i + "/test")));
		}
		System.out.println("remove qos: " + count
				/ Math.max(1, ((System.currentTimeMillis() - t) / 1000)));
		try {
			Thread.sleep(10000000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testDup() {
		sut.add(Topic.asTopic("a/b"));
		System.out.println(sut.dumpTree());
		sut.add(Topic.asTopic("a/b"));
		System.out.println(sut.dumpTree());
	}

	@Test
	public void testMatchSimple() {
		sut.add(Topic.asTopic("/"));
		assertThat(sut.match(asTopic("finance"))).isEmpty();
		System.out.println(sut.dumpTree());
		sut.add(asTopic("/finance"));
		assertThat(sut.match(asTopic("finance"))).isEmpty();
		System.out.println(sut.dumpTree());
		assertThat(sut.match(asTopic("/finance")))
				.contains(asTopic("/finance"));
		assertThat(sut.match(asTopic("/"))).contains(asTopic("/"));
	}

	@Test
	public void testMatchSimpleMulti() {
		sut.add(asTopic("#"));
		assertThat(sut.match(asTopic("finance"))).contains(asTopic("#"));

		sut.add(asTopic("finance/#"));
		assertThat(sut.match(asTopic("finance"))).containsExactlyInAnyOrder(
				asTopic("finance/#"), asTopic("#"));
	}

	@Test
	public void testMatchingDeepMulti_one_layer() {
		Topic anySub = asTopic("#");
		Topic financeAnySub = asTopic("finance/#");
		sut.add(anySub);
		sut.add(financeAnySub);

		// Verify
		assertThat(sut.match(asTopic("finance/stock")))
				.containsExactlyInAnyOrder(financeAnySub, anySub);
		assertThat(sut.match(asTopic("finance/stock/ibm")))
				.containsExactlyInAnyOrder(financeAnySub, anySub);

		System.out.println(sut.dumpTree());
	}

	@Test
	public void testMatchingDeepMulti_two_layer() {
		Topic financeAnySub = asTopic("finance/stock/#");
		sut.add(financeAnySub);

		// Verify
		assertThat(sut.match(asTopic("finance/stock/ibm"))).containsExactly(
				financeAnySub);
	}

	@Test
	public void testMatchSimpleSingle() {
		Topic anySub = asTopic("+");
		sut.add(anySub);
		assertThat(sut.match(asTopic("finance"))).containsExactly(anySub);

		Topic financeOne = asTopic("finance/+");
		sut.add(financeOne);
		assertThat(sut.match(asTopic("finance/stock"))).containsExactly(
				financeOne);
	}

	@Test
	public void testMatchManySingle() {
		Topic manySub = asTopic("+/+");
		sut.add(manySub);

		// verify
		assertThat(sut.match(asTopic("/finance"))).contains(manySub);
	}

	@Test
	public void testMatchSlashSingle() {
		Topic slashPlusSub = asTopic("/+");
		sut.add(slashPlusSub);
		Topic anySub = asTopic("+");
		sut.add(anySub);

		// Verify
		assertThat(sut.match(asTopic("/finance"))).containsOnly(slashPlusSub);
		assertThat(sut.match(asTopic("/finance"))).doesNotContain(anySub);
	}

	@Test
	public void testMatchManyDeepSingle() {
		Topic slashPlusSub = asTopic("/finance/+/ibm");
		sut.add(slashPlusSub);
		Topic slashPlusDeepSub = asTopic("/+/stock/+");
		sut.add(slashPlusDeepSub);

		// Verify
		assertThat(sut.match(asTopic("/finance/stock/ibm")))
				.containsExactlyInAnyOrder(slashPlusSub, slashPlusDeepSub);
	}

	@Test
	public void testMatchSimpleMulti_allTheTree() {
		Topic sub = asTopic("#");
		sut.add(sub);

		assertThat(sut.match(asTopic("finance"))).isNotEmpty();
		assertThat(sut.match(asTopic("finance/ibm"))).isNotEmpty();
	}

	@Test
	public void rogerLightTopicMatches() {
		assertMatch("foo/bar", "foo/bar");
		assertMatch("foo/bar", "foo/bar");
		assertMatch("foo/+", "foo/bar");
		assertMatch("foo/+/baz", "foo/bar/baz");
		assertMatch("foo/+/#", "foo/bar/baz");
		assertMatch("#", "foo/bar/baz");

		assertNotMatch("foo/bar", "foo");
		assertNotMatch("foo/+", "foo/bar/baz");
		assertNotMatch("foo/+/baz", "foo/bar/bar");
		assertNotMatch("foo/+/#", "fo2/bar/baz");

		assertMatch("#", "/foo/bar");
		assertMatch("/#", "/foo/bar");
		assertNotMatch("/#", "foo/bar");

		assertMatch("foo//bar", "foo//bar");
		assertMatch("foo//+", "foo//bar");
		assertMatch("foo/+/+/baz", "foo///baz");
		assertMatch("foo/bar/+", "foo/bar/");
	}

	private void assertMatch(String s, String t) {
		sut = new TopicDirectoryTrie();

		Topic sub = asTopic(s);
		sut.add(sub);

		assertThat(sut.match(asTopic(t))).isNotEmpty();
	}

	private void assertNotMatch(String subscription, String topic) {
		sut = new TopicDirectoryTrie();

		Topic sub = asTopic(subscription);
		sut.add(sub);

		assertThat(sut.match(asTopic(topic))).isEmpty();
	}

	@Test
	public void testOverlappingSubscriptions() {
		Topic sub = asTopic("a/+");
		sut.add(sub);

		Topic sub2 = asTopic("a/b");
		sut.add(sub2);

		// Verify
		assertThat(sut.match(asTopic("a/b")).size()).isEqualTo(2);
	}

	@Test
	public void removeSubscription_withDifferentClients_subscribedSameTopic() {
		Topic slashSub = asTopic("/topic");
		sut.add(slashSub);
		Topic slashSub2 = asTopic("/topic");
		sut.add(slashSub2);
		// Exercise
		sut.remove(asTopic("/topic"));
		// Verify
		Topic remainedSubscription = sut.match(asTopic("/topic")).iterator()
				.next();
		assertNotNull(remainedSubscription);
		assertThat(remainedSubscription).isEqualTo(slashSub);
		sut.remove(asTopic("/topic"));
		assertNotNull(sut.match(asTopic("/topic")));
		assertThat(sut.match(asTopic("/topic")).size()).isEqualTo(0);
	}

	@Test
	public void removeSubscription_sameClients_subscribedSameTopic() {
		Topic slashSub = asTopic("/topic");
		sut.add(slashSub);

		// Exercise
		sut.remove(asTopic("/topic"));

		// Verify
		final Set<Topic> matchingSubscriptions = sut.match(asTopic("/topic"));
		assertThat(matchingSubscriptions).isEmpty();
	}

}
