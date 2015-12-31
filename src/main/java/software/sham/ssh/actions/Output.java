package software.sham.ssh.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Output implements Action {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String output;
    public Output(String output) {
        this.output = output;
    }

    @Override
    public String respond() {
        logger.debug("Sending output: " + output);
        return output;
    }

    @Override
    public String toString() {
        return "output (" + output + ")";
    }
}
