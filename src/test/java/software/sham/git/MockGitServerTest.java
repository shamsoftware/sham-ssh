package software.sham.git;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.password.PasswordIdentityProvider;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.git.transport.GitSshdSessionFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Before;
import org.junit.Test;

import software.sham.MockSshServerTestSupport;

public class MockGitServerTest extends MockSshServerTestSupport {

	private static final String GIT_PROJECT = "mocked-repo";
	private static final String GIT_SSH_URI = String.format("ssh://%s@%s:%d/%s", //
			SSH_SERVER_USER, SSH_SERVER_HOSTNAME, SSH_SERVER_PORT, GIT_PROJECT);

	private static final UsernamePasswordCredentialsProvider CREDENTIALS_PROVIDER = new UsernamePasswordCredentialsProvider(
			SSH_SERVER_USER, SSH_SERVER_PASSWORD);

	private SshClient client;

	public MockGitServerTest() {
		super(() -> new MockGitServer(MockSshServerTestSupport.SSH_SERVER_PORT));
	}

	@Before
	public void createSshClient() {
		client = SshClient.setUpDefaultClient();
		client.setPasswordIdentityProvider(new PasswordIdentityProvider() {
			@Override
			public Iterable<String> loadPasswords(SessionContext session) throws IOException, GeneralSecurityException {
				return Collections.singleton(SSH_SERVER_PASSWORD);
			}
		});
		client.start();

		SshSessionFactory gitSessionFactory = new GitSshdSessionFactory(client);
		SshSessionFactory.setInstance(gitSessionFactory);
	}

	@Test
	public void canConnect() throws IOException {
		ConnectFuture connectFuture = client.connect(GIT_SSH_URI);
		assertTrue(connectFuture.await());
		assertTrue(connectFuture.isConnected());

		ClientSession session = connectFuture.getSession();
		AuthFuture authFuture = session.auth();
		assertTrue(authFuture.await());
		assertTrue(authFuture.isSuccess());
	}

	@Test
	public void connectAndFetch()
			throws InvalidRemoteException, TransportException, GitAPIException, IOException, InterruptedException {
		File directory = Files.createTempDirectory(getClass().getSimpleName()).toFile();
		directory.deleteOnExit();

		getServer().prepareGitProject(GIT_PROJECT);

		Git git = Git.cloneRepository().setURI(GIT_SSH_URI).setDirectory(directory)
				// surprisingly JGIT wouldn't take sshClient setup
				.setCredentialsProvider(CREDENTIALS_PROVIDER).call();

		git.fetch().setCredentialsProvider(CREDENTIALS_PROVIDER).call();

		Iterable<RevCommit> commits = git.log().call();
		assertTrue(commits.iterator().hasNext());
	}

	public MockGitServer getServer() {
		return (MockGitServer) super.server;
	}
}
