package software.sham.ssh.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Delay implements Action {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final long milliseconds;

    public Delay(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    @Override
    public String respond() {
        try {
            logger.debug("Delaying output for {}ms", milliseconds);
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            logger.warn("Interrupted before delay completed: {}", e.getMessage());
        }
        return "";
    }

    @Override
    public String toString() {
        return "delay (" + milliseconds + "ms)";
    }
}
