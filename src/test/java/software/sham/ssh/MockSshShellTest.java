package software.sham.ssh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sshtools.client.shell.ExpectShell;
import com.sshtools.client.tasks.ShellTask;
import com.sshtools.client.tasks.ShellTask.ShellTaskBuilder;
import com.sshtools.client.tasks.Task;
import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.ssh.SshException;

import software.sham.MockSshServerTestSupport;

public class MockSshShellTest extends MockSshServerTestSupport {

	private static final long TASK_TIMEOUT = 5000L;

	public MockSshShellTest() {
		super(server -> {
			server.enableShell();
		});
	}

	@BeforeClass
	public static void setLoggers() {
		Log.enableConsole(Level.DEBUG);
		// see src/test/resources/simplelogger.properties
	}

	@Before
	public void initSshClientWithPassword() throws SshException, IOException {
		super.initSshClientWithPassword();
	}

	@Test
	public void shouldBeGreetedAndExit() throws IOException {
		runInShell(shell -> {
			// force shell readiness with first command
			assertEquals("Hi", shell.executeWithOutput("echo Hi"));
		});

		Awaitility.await().until(() -> Boolean.FALSE.equals(sshClient.isConnected()));
	}

	@Test
	public void differentOutputForDifferentInput() throws IOException {
		server.respondTo("Something wicked this way comes").withOutput("default");
		server.respondTo("Knock knock").withOutput("Who's there?");

		runInShell(shell -> {
			assertEquals("default", shell.executeWithOutput("Something wicked this way comes"));

			assertEquals("Who's there?", shell.executeWithOutput("Knock knock"));
		});
	}

	@Test
	public void delayedOutput() throws IOException {
		server.respondTo("delayedResponder").withOutput("Starting..." + System.lineSeparator()).withDelay(500L)
				.withOutput("Completed.");

		runInShell(shell -> {
			assertEquals("Starting..." + System.lineSeparator() + System.lineSeparator() + "Completed.",
					shell.executeWithOutput("delayedResponder"));
		});
	}

	/**
	 * SMI for testing purpose
	 */
	interface TestInShell {
		void test(ExpectShell shell) throws IOException, SshException;
	}

	private void runInShell(TestInShell shellTest) throws IOException {
		ShellTask task = ShellTaskBuilder.create() //
				.withClient(sshClient) //
				.onTask((t, session) -> {
					ExpectShell shell = new ExpectShell(t, ExpectShell.OS_LINUX);

					shellTest.test(shell);

					// potentially forceful quitting
					shell.exit();
				}) //
				.build();

		Task clientTask = sshClient.addTask(task);

		RequestFuture requestFuture = clientTask // .waitForever();
				.waitFor(TASK_TIMEOUT);

		assertTrue(clientTask.getLastError() != null ? clientTask.getLastError().toString() : "No success.", //
				requestFuture.isDoneAndSuccess());
	}

}