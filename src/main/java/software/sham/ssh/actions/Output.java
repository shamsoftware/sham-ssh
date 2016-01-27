package software.sham.ssh.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.sham.ssh.MockSshShell;

public class Output implements Action {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String output;
    public Output(String output) {
        this.output = output;
    }

    @Override
    public void respond(MockSshShell shell) {
        logger.debug("Sending output: " + output);
        shell.sendResponse(output);
    }

    @Override
    public String toString() {
        return "output (" + output + ")";
    }
}
