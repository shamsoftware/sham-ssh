package software.sham.ssh;

import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ResponderDispatcher {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<Matcher, SshResponder> responders = new HashMap<>();
    private final LinkedList<Matcher> matchers = new LinkedList<>();

    public void add(Matcher matcher, SshResponder responder) {
        matchers.add(0, matcher); // matchers added last take precedence
        responders.put(matcher, responder);
    }

    public SshResponder find(String input) {
        for (Matcher matcher : matchers) {
            logger.debug("checking {}", matcher.toString());
            if (matcher.matches(input)) {
                logger.debug("Found responder for " + matcher.toString());
                return responders.get(matcher);
            } else {
                logger.debug("did not match " +matcher.toString());
            }
        }
        logger.info("No responder found for input " + input);
        return SshResponder.NULL;
    }
}
