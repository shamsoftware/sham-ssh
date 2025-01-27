package software.sham.ssh;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.sham.ssh.actions.Action;

class SshResponder {
	public static final SshResponder NULL = new SshResponder();
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final List<Action> actions = new LinkedList<>();

	public void respond(ServerSession serverSession, String input, OutputStream outputStream) {
		for (Action action : actions) {
			try {
				action.respond(serverSession, input, outputStream);
			} catch (IOException e) {
				logger.warn("Mock SSH error during response {}: {}", action.toString(), e.getMessage());
			}
		}
	}

	public SshResponder add(Action action) {
		actions.add(action);
		return this;
	}

}
