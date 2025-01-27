package software.sham.ssh.actions;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.sshd.server.session.ServerSession;

public class Prompt extends OutputSupport implements Action {

	private final String promptFormat;

	/**
	 * "{0}" <- {@link ServerSession#getUsername()}
	 * 
	 * @param prompt
	 * @see MessageFormat
	 */
	public Prompt(String prompt) {
		this.promptFormat = prompt;
	}

	public Prompt() {
		this.promptFormat = "{0}@shell-$ ";
	}

	@Override
	public void respond(ServerSession serverSession, OutputStream outputStream) throws IOException {
		String prompt = MessageFormat.format(promptFormat, serverSession.getUsername());

		super.write(prompt, outputStream);
	}
}