package software.sham.ssh;

public class SshResponderBuilder {
    private final SshResponder responder = new SshResponder();

    public SshResponderBuilder withText(String response) {
        responder.setResponse(response);
        return this;
    }

    public SshResponder getResponder() {
        return this.responder;
    }
}
