package software.sham.ssh;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * dispatcher.matchers (reverse-order) -> responder
 * 
 * TODO pass-through input
 */
class ResponderDispatcher {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Map<Matcher<String>, SshResponder> responders = new HashMap<>();
	private final LinkedList<Matcher<String>> matchers = new LinkedList<>();

	public SshResponderBuilder respondTo(Matcher<String> matcher) {
		SshResponderBuilder builder = SshResponderBuilder.builder();
		this.add(matcher, builder.getResponder());
		return builder;
	}

	public SshResponderBuilder respondTo(String input) {
		return this.respondTo(Matchers.equalTo(input));
	}

	public SshResponder find(String input) {
		for (Matcher<String> matcher : matchers) {
			logger.debug("checking {}", matcher.toString());
			if (matcher.matches(input)) {
				logger.debug("Found responder for " + matcher.toString());
				return responders.get(matcher);
			} else {
				logger.debug("did not match " + matcher.toString());
			}
		}
		logger.info("No responder found for input " + input);
		return SshResponder.NULL;
	}

	private void add(Matcher<String> matcher, SshResponder responder) {
		matchers.add(0, matcher); // matchers added last take precedence
		responders.put(matcher, responder);
	}

	public void clear() {
		matchers.clear();
		responders.clear();
	}
}
