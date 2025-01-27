package software.sham.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.channel.ChannelSession;
import org.hamcrest.Matchers;

import software.sham.ssh.actions.Action;
import software.sham.ssh.actions.Greet;
import software.sham.ssh.actions.Prompt;

class MockSshShell extends CommandParserSupport {

	private final ResponderDispatcher dispatcher;

	private final Action greet = new Greet();
	private final Action prompt = new Prompt();

	protected MockSshShell(ResponderDispatcher dispatcher) {
		super("shell", null);

		this.dispatcher = dispatcher;
	}

	@Override
	public void start(ChannelSession channelSession, Environment env) throws IOException {
		greet.respond(getServerSession(), getOutputStream());

		executorService.submit(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()))) {
				while (super.getServerSession().isOpen()) {
					String[] commands = super.divideCommands(reader.readLine());
					for (String command : commands) {
						SshResponder responder = dispatcher.find(command.trim());
						responder.respond(getServerSession(), command, getOutputStream());
					}

					prompt.respond(getServerSession(), getOutputStream());

					getOutputStream().flush();
				}
			} catch (IOException ex) {
				log.error("Shell aborting due to Exception.", ex);
			}
		});
	}

	@Override
	public void destroy(ChannelSession channelSession) throws Exception {
		executorService.shutdown();
	}

	@Override
	public void run() {
		// do nothing here
	}

	/**
	 * Activate mocked behavior for
	 * <ul>
	 * <li>basic `echo` (including mocked $?)</li>
	 * <li>`exit` closing connection from server-end</li>
	 * </ul>
	 */
	public void setDefaults() {
		this.dispatcher.respondTo(Matchers.startsWith("echo")).withEcho();

		this.dispatcher.respondTo("exit").withClose();
	}
}
