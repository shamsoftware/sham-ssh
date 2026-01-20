package software.sham.ssh.actions;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.sshd.server.session.ServerSession;

public class Greet extends OutputSupport implements Action {

	private final String greeting;

	public Greet() {
		// e.g. Welcome to Ubuntu 24.04.1 LTS (GNU/Linux 6.8.0-51-generic x86_64)
		// TODO bring maven-artifact-version via properties file into this spot
		this.greeting = String.format("Welcome to %s (GNU/Linux %s x86_64)\n", //
				this.getClass().getSimpleName(), //
				"1.2.3" //
		);
	}

	public Greet(String greeting) {
		this.greeting = greeting;
	}

	@Override
	public void respond(ServerSession serverSession, OutputStream outputStream) throws IOException {
		super.write(greeting, outputStream);
	}
}
