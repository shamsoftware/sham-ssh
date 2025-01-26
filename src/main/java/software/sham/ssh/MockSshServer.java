package software.sham.ssh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;

import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ShellFactory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockSshServer implements ShellFactory {
	public static final String USERNAME = "tester";
	public static final String PASSWORD = "testing";

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	protected final SshServer sshServer;

	private Set<PublicKey> keys = new HashSet<PublicKey>();
	private MockSshShell sshShell;

	public MockSshServer(int port) throws IOException {
		this(port, true);
	}

	public MockSshServer(int port, boolean shouldStartServices) throws IOException {
		sshServer = initSshServer(port);
		if (shouldStartServices) {
			enableShell();
			start();
		}
	}

	/**
	 * @param key with explicit {@link X509EncodedKeySpec#getAlgorithm()} notion
	 */
	public MockSshServer allowPublicKey(X509EncodedKeySpec keySpec) throws GeneralSecurityException {
		this.keys.add(new PublicKeyRetriever(keySpec).getPublicKey());
		sshServer.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(this, this.keys));
		return this;

	}

	public MockSshServer enableShell() {
		log.info("Mock SSH shell is enabled");
		sshShell = new MockSshShell();
		setDefaults();
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

	protected SshServer initSshServer(int port) {
		final SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setPort(port);
		sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
			@Override
			public boolean authenticate(String username, String password, ServerSession session) {
				return USERNAME.equals(username) && PASSWORD.equals(password);
			}

		});
		sshd.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(this, this.keys));
		return sshd;
	}

	public SshResponderBuilder respondTo(Matcher<String> matcher) {
		SshResponderBuilder builder = new SshResponderBuilder();
		sshShell.getDispatcher().add(matcher, builder.getResponder());
		return builder;
	}

	public SshResponderBuilder respondTo(String input) {
		return respondTo(Matchers.equalTo(input));
	}

	private void setDefaults() {
		respondTo("exit").withClose();
	}

	@Override
	public Command createShell(ChannelSession channel) throws IOException {
		return this.sshShell; // mocked singleton
	}
}
