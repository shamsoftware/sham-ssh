package software.sham.ssh;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class FunctionalTest {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	MockSshServer server;
	Session sshSession;
	ChannelShell sshChannel;
	ByteArrayOutputStream sshClientOutput;
	PrintWriter inputWriter;

	@Before
	public void initSsh() throws IOException {
		server = new MockSshServer(9022);
	}

	@Before
	public void initSshClientWithPassword() throws JSchException, IOException {
		initSshClient();

		sshSession.setPassword("testing");

		connectWithStreams();
	}

	private void initSshClientWithKey() throws JSchException, IOException {
		JSch jsch = initSshClient();

		jsch.addIdentity("src/test/resources/keys/id_rsa_tester", "testing");

		connectWithStreams();
	}

	private JSch initSshClient() throws JSchException {
		JSch jsch = new JSch();
		sshSession = jsch.getSession("tester", "localhost", 9022);
		Properties config = new Properties();
		config.setProperty("StrictHostKeyChecking", "no");
		sshSession.setConfig(config);
		return jsch;
	}

	private void connectWithStreams() throws JSchException, IOException {
		sshSession.connect();
		sshChannel = (ChannelShell) sshSession.openChannel("shell");
		PipedInputStream channelIn = new PipedInputStream();
		sshChannel.setInputStream(channelIn);
		OutputStream sshClientInput = new PipedOutputStream(channelIn);
		inputWriter = new PrintWriter(sshClientInput);
		sshClientOutput = new ByteArrayOutputStream();
		sshChannel.setOutputStream(sshClientOutput);
		sshChannel.connect(1000);
	}

	@After
	public void stopSsh() throws Exception {
		sshSession.disconnect();
		server.stop();
	}

	@Test
	public void defaultShellCommandsShouldSilentlySucceed() throws Exception {
		sendTextToServer("Knock knock\n");

		waitFor(() -> sshClientOutput.size(), equalTo(0));
	}

	@Test
	public void defaultShellShouldDisconnectOnExit() throws Exception {
		sendTextToServer("exit\n");

		waitFor(() -> sshChannel.isConnected(), is(false));
	}

	@Test
	public void singleOutput() throws Exception {
		server.respondTo(any(String.class)).withOutput("hodor\n");

		sendTextToServer("Knock knock\n");

		waitForOutput("hodor\n");
	}

	@Test
	public void shouldSupportPublicKeyAuth() throws Exception {
		server.allowPublicKey( //
				Files.readAllBytes( //
						Path.of( //
								Thread.currentThread().getContextClassLoader() //
										.getResource("keys/id_rsa_tester.der.pub").toURI())));

		initSshClientWithKey();

		server.respondTo(any(String.class)).withOutput("hodor\n");

		sendTextToServer("Knock knock\n");

		waitForOutput("hodor\n");
	}

	@Test
	public void multipleOutput() throws Exception {
		server.respondTo(any(String.class)).withOutput("Starting...\n").withOutput("Completed.\n");

		sendTextToServer("start");
		Thread.sleep(100L);

		waitForOutput("Starting...\nCompleted.\n");
	}

	@Test
	public void delayedOutput() throws Exception {
		server.respondTo(any(String.class)).withOutput("Starting...\n").withDelay(500L).withOutput("Completed.\n");

		sendTextToServer("start");
		logger.debug("Checking for first line");
		waitForOutput("Starting...\n");
		logger.debug("Checking for second line");
		waitForOutput("Starting...\nCompleted.\n");
	}

	@Test
	public void differentOutputForDifferentInput() throws Exception {
		server.respondTo(any(String.class)).withOutput("default\n");
		server.respondTo("Knock knock").withOutput("Who's there?\n");

		sendTextToServer("Something wicked this way comes");
		waitForOutput("default\n");

		sendTextToServer("Knock knock");
		waitForOutput("default\nWho's there?\n");
	}

	private void sendTextToServer(final String text) throws Exception {
		inputWriter.write(text);
		inputWriter.flush();
		logger.debug("Sent text to SSH server: {}", text);
	}

	private <T> T waitFor(final Callable<T> supplier, final Matcher<? super T> matcher) {
		return await() //
				.during(200, TimeUnit.MICROSECONDS) //
				.atMost(1, TimeUnit.SECONDS) //
				.until(supplier, matcher);
	}

	private String waitForOutput(String output) {
		return this.<String>waitFor(() -> sshClientOutput.toString(), equalTo(output));
	}

}
