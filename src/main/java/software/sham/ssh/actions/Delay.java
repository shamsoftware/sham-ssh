package software.sham.ssh.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.sham.ssh.MockSshShell;

public class Delay implements Action {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final long milliseconds;

    public Delay(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    @Override
    public void respond(MockSshShell shell) {
        try {
            logger.debug("Delaying output for {}ms", milliseconds);
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            logger.warn("Interrupted before delay completed: {}", e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "delay (" + milliseconds + "ms)";
    }
}
