package com.eastelsoft.etos2.mqtt.server;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eet.evar.StringDeal;
import eet.evar.tool.logger.Logger;
import eet.evar.tool.logger.LoggerFactory;

public class Topic implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(Topic.class);

	private static final long serialVersionUID = 2438799283749822L;

	public static final String TOKEN_SPILIT = "/";

	private String topicFilter;

	@JsonIgnore
	private transient List<Token> tokens;

	@JsonIgnore
	private transient boolean valid;

	public static Topic asTopic(String s) {
		return new Topic(s);
	}

	public Topic() {
		topicFilter = null;
	}

	public Topic(String topicFilter) {
		this.topicFilter = topicFilter;
	}

	Topic(List<Token> tokens) {
		this.tokens = tokens;
		if (tokens != null && !tokens.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (Token token : tokens) {
				sb.append(token.toString()).append(TOKEN_SPILIT);
			}
			this.topicFilter = sb.substring(0, sb.length() - 1);
		} else {
			this.topicFilter = null;
		}
		this.valid = true;
	}

	@JsonIgnore
	public List<Token> getTokens() {
		if (tokens == null) {
			try {
				tokens = parseTopic(topicFilter);
				valid = true;
			} catch (ParseException e) {
				valid = false;
				logger.error("Error parsing the topic: {}, message: {}", topicFilter,
						e.getMessage());
			}
		}
		return tokens;
	}

	private List<Token> parseTopic(String topic) throws ParseException {
		List<Token> res = new ArrayList<Token>();
		String[] splitted = topic.split(TOKEN_SPILIT);// StringDeal.split(topic,
														// TOKEN_SPILIT);

		if (splitted == null || splitted.length == 0) {
			res.add(Token.EMPTY);
		}
		if (topic.endsWith("/")) {
			// Add a fictious space
			String[] newSplitted = new String[splitted.length + 1];
			System.arraycopy(splitted, 0, newSplitted, 0, splitted.length);
			newSplitted[splitted.length] = "";
			splitted = newSplitted;
		}
		for (int i = 0; i < splitted.length; i++) {
			String s = splitted[i];
			if (s.isEmpty()) {
				res.add(Token.EMPTY);
			} else if (s.equals("#")) {
				// check that multi is the last symbol
				if (i != splitted.length - 1) {
					throw new ParseException(
							"Bad format of topic, the multi symbol (#) has to be the last one after a separator",
							i);
				}
				res.add(Token.MULTI);
			} else if (s.contains("#")) {
				throw new ParseException(
						"Bad format of topic, invalid subtopic name: " + s, i);
			} else if (s.equals("+")) {
				res.add(Token.SINGLE);
			} else if (s.contains("+")) {
				throw new ParseException(
						"Bad format of topic, invalid subtopic name: " + s, i);
			} else {
				res.add(new Token(s));
			}
		}

		return res;
	}

	public Token headToken() {
		final List<Token> tokens = getTokens();
		if (tokens.isEmpty()) {
			// TODO UGLY use Optional
			return null;
		}
		return tokens.get(0);
	}

	@JsonIgnore
	public boolean isEmpty() {
		final List<Token> tokens = getTokens();
		return tokens == null || tokens.isEmpty();
	}

	/**
	 * @return a new Topic corresponding to this less than the head token
	 * */
	public Topic exceptHeadToken() {
		List<Token> tokens = getTokens();
		if (tokens.isEmpty()) {
			return new Topic(Collections.emptyList());
		}
		List<Token> tokensCopy = new ArrayList<Token>(tokens);
		tokensCopy.remove(0);
		return new Topic(tokensCopy);
	}

	@JsonIgnore
	public boolean isValid() {
		if (tokens == null) {
			getTokens();
		}
		return valid;
	}

	public boolean containWildcard() {
		if (tokens == null) {
			getTokens();
		}
		for (Token token : tokens) {
			if (token == Token.MULTI || token == Token.SINGLE) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verify if the 2 topics matching respecting the rules of MQTT Appendix A
	 *
	 * @param subscriptionTopic
	 *            the topic filter of the subscription
	 * @return true if the two topics match.
	 */
	// TODO reimplement with iterators or with queues
	public boolean match(Topic subscriptionTopic) {
		List<Token> msgTokens = getTokens();
		List<Token> subscriptionTokens = subscriptionTopic.getTokens();
		int i = 0;
		for (; i < subscriptionTokens.size(); i++) {
			Token subToken = subscriptionTokens.get(i);
			if (subToken != Token.MULTI && subToken != Token.SINGLE) {
				if (i >= msgTokens.size()) {
					return false;
				}
				Token msgToken = msgTokens.get(i);
				if (!msgToken.equals(subToken)) {
					return false;
				}
			} else {
				if (subToken == Token.MULTI) {
					return true;
				}
				if (subToken == Token.SINGLE) {
					// skip a step forward
				}
			}
		}
		// if last token was a SINGLE then treat it as an empty
		// if (subToken == Token.SINGLE && (i - msgTokens.size() == 1)) {
		// i--;
		// }
		return i == msgTokens.size();
	}
	
	public String getTopicFilter() {
		return topicFilter;
	}

	public void setTopicFilter(String topicFilter) {
		this.topicFilter = topicFilter;
	}

	@Override
	public String toString() {
		return topicFilter;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Topic other = (Topic) obj;

		return Objects.equals(this.topicFilter, other.topicFilter);
	}

	@Override
	public int hashCode() {
		return topicFilter.hashCode();
	}

}
