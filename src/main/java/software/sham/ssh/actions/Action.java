package software.sham.ssh.actions;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.sshd.server.session.ServerSession;

/**
 * callers must use {@link #respond(ServerSession, String, OutputStream)}
 */
public interface Action {
	default void respond() {
		throw new RuntimeException("not implemented");
	}
	
	default void respond(ServerSession serverSession) throws IOException {
		respond();
	}

	default void respond(ServerSession serverSession, OutputStream outputStream) throws IOException {
		respond(serverSession);
	};

	default void respond(ServerSession serverSession, String input, OutputStream outputStream) throws IOException {
		respond(serverSession, outputStream);
	};
}
