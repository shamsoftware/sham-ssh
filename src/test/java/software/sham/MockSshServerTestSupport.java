package software.sham;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.common.ssh.SshException;

import software.sham.ssh.MockSshServer;

public abstract class MockSshServerTestSupport {

	protected static final String SSH_SERVER_HOSTNAME = "localhost";
	protected static final int SSH_SERVER_PORT = 9022;
	protected static final String SSH_SERVER_USER = MockSshServer.USERNAME;
	protected static final String SSH_SERVER_PASSWORD = MockSshServer.PASSWORD;
	protected static final String SSH_SERVER_USER_KEY_PASSPHRASE = "testing";
	protected static final long SSH_SERVER_CONNECT_TIMOUT = 1000L;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected MockSshServer server;
	protected SshClient sshClient;

	protected interface MockSshServerConfigurer {
		void apply(MockSshServer server);
	}

	protected interface MockSshServerCreator {
		MockSshServer create() throws IOException;
	}

	MockSshServerConfigurer serverConfigurer = server -> {
		/* do nothing */ };
	private MockSshServerCreator serverCreator = () -> {
		return new MockSshServer(MockSshServerTestSupport.SSH_SERVER_PORT, false);
	};

	protected MockSshServerTestSupport(MockSshServerCreator serverCreator, MockSshServerConfigurer serverConfigurer) {
		this.serverCreator = serverCreator;
		this.serverConfigurer = serverConfigurer;
	}

	protected MockSshServerTestSupport(MockSshServerCreator serverCreator) {
		this.serverCreator = serverCreator;
	}

	protected MockSshServerTestSupport(MockSshServerConfigurer serverConfigurer) {
		this.serverConfigurer = serverConfigurer;
	}

	protected MockSshServerTestSupport() {
		// no specific serverConfigurer
	}

	@Before
	public void startSshServer() throws IOException {
		this.server = this.serverCreator.create();
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
