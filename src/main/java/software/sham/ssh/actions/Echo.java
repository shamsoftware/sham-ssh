package software.sham.ssh.actions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sshd.server.session.ServerSession;

public class Echo extends OutputSupport implements Action {

	private static final Pattern ECHO_PATTERN = Pattern.compile("[ ]*echo[ ]+\"?([^\"]+)\"?");
	private final int returnCode;

	public Echo() {
		this.returnCode = 0;
	}

	public Echo(int returnCode) {
		this.returnCode = returnCode;
	}

	@Override
	public void respond(ServerSession serverSession, String input, OutputStream outputStream) throws IOException {
		Matcher matcher = ECHO_PATTERN.matcher(input);

		while (matcher.find()) {
			String output = matcher.group(1) //
					.replace("$?", String.valueOf(returnCode));
			super.write(output, outputStream);
		}
	}

}
