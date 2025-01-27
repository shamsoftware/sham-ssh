package software.sham.ssh;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.common.ssh.SshException;

abstract class MockSshServerTestSupport {

	static final String SSH_SERVER_HOSTNAME = "localhost";
	static final int SSH_SERVER_PORT = 9022;

	static final String SSH_SERVER_USER = MockSshServer.USERNAME;
	static final String SSH_SERVER_PASSWORD = MockSshServer.PASSWORD;
	static final String SSH_SERVER_USER_KEY_PASSPHRASE = "testing";

	static final long SSH_SERVER_CONNECT_TIMOUT = 1000L;
	// private static final long SSH_SERVER_SHELL_WAIT = 1000L;

	final Logger logger = LoggerFactory.getLogger(getClass());

	MockSshServer server;
	SshClient sshClient;

	interface MockSshServerConfigurer {
		void apply(MockSshServer server);
	}

	MockSshServerConfigurer serverConfigurer = server -> {
		/* do nothing */ };

	MockSshServerTestSupport(MockSshServerConfigurer serverConfigurer) {
		this.serverConfigurer = serverConfigurer;
	}

	MockSshServerTestSupport() {
		// no specific serverConfigurer
	}

	@Before
	public void startSshServer() throws IOException {
		this.server = new MockSshServer(MockSshServerTestSupport.SSH_SERVER_PORT, false);
		this.serverConfigurer.apply(server);
		this.server.start();
	}

	@After
	public void stopSsh() throws IOException, InterruptedException {
		if (sshClient != null)
			this.sshClient.close();
		this.server.stop();
	}

	protected void initSshClientWithPassword() throws SshException, IOException {
		this.sshClient = SshClientBuilder.create() //
				.withTarget(MockSshServerTestSupport.SSH_SERVER_HOSTNAME, MockSshServerTestSupport.SSH_SERVER_PORT) //
				.withUsername(MockSshServerTestSupport.SSH_SERVER_USER) //
				.withPassword(MockSshServerTestSupport.SSH_SERVER_PASSWORD) //
				.build();

		this.sshClient.openSessionChannel(MockSshServerTestSupport.SSH_SERVER_CONNECT_TIMOUT);
	}

	protected String sendTextToServer(final String text) throws IOException {
		return this.sshClient.executeCommand(text);
	}
}
