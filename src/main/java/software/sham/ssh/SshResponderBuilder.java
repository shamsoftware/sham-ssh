package software.sham.ssh;

import software.sham.ssh.actions.Delay;
import software.sham.ssh.actions.Output;

public class SshResponderBuilder {
    private final SshResponder responder = new SshResponder();

    public SshResponder getResponder() {
        return this.responder;
    }

    public SshResponderBuilder withOutput(String output) {
        responder.add(new Output(output));
        return this;
    }

    public SshResponderBuilder withDelay(long milliseconds) {
        responder.add(new Delay(milliseconds));
        return this;
    }
}
