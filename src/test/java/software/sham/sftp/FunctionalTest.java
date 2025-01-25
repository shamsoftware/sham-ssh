package software.sham.sftp;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class FunctionalTest {
	MockSftpServer server;
	Session sshSession;

	@Before
	public void initSftp() throws IOException {
		server = new MockSftpServer(9022);
	}

	@Before
	public void initSshClient() throws JSchException {
		JSch jsch = new JSch();
		sshSession = jsch.getSession("tester", "localhost", 9022);
		Properties config = new Properties();
		config.setProperty("StrictHostKeyChecking", "no");
		sshSession.setConfig(config);
		sshSession.setPassword("testing");
		sshSession.connect();
	}

	@After
	public void stopSftp() throws IOException {
		server.stop();
	}

	@Test
	public void connectAndDownloadFile() throws JSchException, IOException, SftpException {
		Files.write(server.getBaseDirectory().resolve("example.txt"),
				Collections.singletonList("example file contents"));

		ChannelSftp channel = (ChannelSftp) sshSession.openChannel("sftp");
		channel.connect();

		BufferedReader reader = new BufferedReader(new InputStreamReader(channel.get("example.txt")));
		final String downloadedContents = reader.readLine();
		
		assertEquals("example file contents", downloadedContents);
	}
}
