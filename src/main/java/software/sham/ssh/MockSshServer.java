package software.sham.ssh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.AbstractCommandSupport;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ShellFactory;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsInstanceOf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockSshServer implements ShellFactory, CommandFactory {
	public static final String USERNAME = "tester";
	public static final String PASSWORD = "testing";

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	private final Set<PublicKey> keys = new HashSet<PublicKey>();
	private final ResponderDispatcher dispatcher = new ResponderDispatcher();
	private final SshServer sshServer;
	private MockSshShell sshShell;

	public MockSshServer(int port) throws IOException {
		this(port, true);
	}

	public MockSshServer(int port, boolean shouldStartServices) throws IOException {
		this.sshServer = SshServer.setUpDefaultServer();
		this.sshServer.setPort(port);

		this.sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {
			@Override
			public boolean authenticate(String username, String password, ServerSession session) {
				return USERNAME.equals(username) && PASSWORD.equals(password);
			}

		});
		this.sshServer.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(this, this.keys));

		this.sshServer.setCommandFactory(this);

		if (shouldStartServices) {
			enableShell();
			start();
		}
	}

	public MockSshServer allowPublicKey(PublicKey publicKey) {
		this.keys.add(publicKey);
		return this;
	}

	/**
	 * enables shell which basically reacts to `exit` and `echo`.
	 * 
	 * @return fluent-api
	 */
	public MockSshServer enableShell() {
		log.info("Mock SSH shell is enabled");
		sshShell = new MockSshShell(this.dispatcher);
		sshShell.setDefaults();
		sshServer.setShellFactory(this);
		return this;
	}

	public void start() throws IOException {
		Path serverKeyPath = Files.createTempFile("sham-mock-sshd-key", null);
		AbstractGeneratorHostKeyProvider keyProvider = SecurityUtils.createGeneratorHostKeyProvider(serverKeyPath);
		sshServer.setKeyPairProvider(keyProvider);

		sshServer.start();
	}

	public void stop() throws IOException {
		sshServer.stop();
	}

	public SshResponderBuilder respondTo(Matcher<String> matcher) {
		if (IsInstanceOf.class.equals(matcher.getClass()))
			log.warn("Be careful with 'any' matcher, it may harm basic functions.");
		return this.dispatcher.respondTo(matcher);
	}

	public SshResponderBuilder respondTo(String input) {
		return this.dispatcher.respondTo(input);
	}

	@Override
	public Command createShell(ChannelSession channel) throws IOException {
		return this.sshShell; // mocked singleton
	}

	@Override
	public Command createCommand(final ChannelSession channel, String command) throws IOException {
		return new AbstractCommandSupport(command, null) {

			@Override
			public void run() {
				try {
					dispatcher.find(command).respond(getServerSession(), command, getOutputStream());
					Thread.sleep(100L); // graceful wait for client read
					onExit(0);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		};
	}

	protected SshServer getSshServer() {
		return sshServer;
	}
}
