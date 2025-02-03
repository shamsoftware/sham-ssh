package software.sham.ssh;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.sshtools.common.ssh.SshException;

import software.sham.MockSshServerTestSupport;

public class MockSshCommandTest extends MockSshServerTestSupport {

	@Before
	public void initSshClientWithPassword() throws SshException, IOException {
		super.initSshClientWithPassword();
	}

	@Test
	public void defaultShellCommandsShouldSilentlySucceed() throws IOException {
		assertThat(sendTextToServer("Knock knock"), emptyString());
	}

	@Test
	public void singleOutput() throws IOException {
		server.respondTo(any(String.class)).withOutput("hodor");

		assertEquals("hodor\n", sendTextToServer("Knock knock"));
	}

	@Test
	public void multipleOutput() throws IOException {
		server.respondTo(any(String.class)).withOutput("Starting...").withOutput("Completed.");

		assertEquals("Starting...\nCompleted.\n", sendTextToServer("start"));
	}

	@Test
	public void multilineOutput() throws IOException {
		server.respondTo(any(String.class)).withOutput("Starting...", "Completed.");

		assertEquals("Starting...\nCompleted.\n", sendTextToServer("start"));
	}
}
