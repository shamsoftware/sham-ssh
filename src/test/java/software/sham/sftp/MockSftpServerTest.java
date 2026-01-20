package software.sham.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Test;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

import software.sham.MockSshServerTestSupport;

public class MockSftpServerTest extends MockSshServerTestSupport {

	static final String TEST_FILE = "example.txt";
	static final String TEST_FILE_CONTENT = "example file contents" + System.lineSeparator();

	private SftpClient sftpClient;

	public MockSftpServerTest() {
		super( //
				() -> new MockSftpServer(MockSshServerTestSupport.SSH_SERVER_PORT), //
				server -> server.enableShell() //
		);
	}

	@Before
	public void initSshClient() throws SshException, PermissionDeniedException, IOException {
		super.initSshClientWithPassword();
		this.sftpClient = SftpClientBuilder.create().withClient(this.sshClient).build();
	}

	@Test
	public void connectAndDownloadFile()
			throws IOException, SftpStatusException, SshException, TransferCancelledException {
		Files.write( //
				getSftpServer().getBaseDirectory().resolve(TEST_FILE), //
				TEST_FILE_CONTENT.getBytes(Charset.defaultCharset()) //
		);

		assertTrue(sftpClient.exists(TEST_FILE));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		sftpClient.get(TEST_FILE, baos);

		assertEquals(TEST_FILE_CONTENT, baos.toString());

		sftpClient.close();
	}

	protected MockSftpServer getSftpServer() {
		return (MockSftpServer) super.server;
	}
}
