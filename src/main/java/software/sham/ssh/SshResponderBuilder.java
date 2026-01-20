package software.sham.ssh;

import software.sham.ssh.actions.Close;
import software.sham.ssh.actions.Delay;
import software.sham.ssh.actions.Echo;
import software.sham.ssh.actions.Output;

/**
 * 
 */
public class SshResponderBuilder {
	private final SshResponder responder = new SshResponder();

	public static SshResponderBuilder builder() {
		return new SshResponderBuilder();
	}

	public SshResponder getResponder() {
		return this.responder;
	}

	public SshResponderBuilder withOutput(String... multiLineOutput) {
		responder.add(new Output(multiLineOutput));
		return this;
	}

	public SshResponderBuilder withDelay(long milliseconds) {
		responder.add(new Delay(milliseconds));
		return this;
	}

	public SshResponderBuilder withClose() {
		responder.add(new Close());
		return this;
	}

	public SshResponderBuilder withEcho() {
		responder.add(new Echo());
		return this;
	}
}
