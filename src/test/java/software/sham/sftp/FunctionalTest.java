package software.sham.sftp;

import static org.junit.Assert.*;

import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

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
        Files.copy(IOUtils.toInputStream("example file contents"), server.getBaseDirectory().resolve("example.txt"));

        ChannelSftp channel = (ChannelSftp) sshSession.openChannel("sftp");
        channel.connect();

        final String downloadedContents = IOUtils.toString(channel.get("example.txt"));
        assertEquals("example file contents", downloadedContents);
    }

    @Test
    public void connectAndUploadFile() throws JSchException, SftpException, IOException {
        ChannelSftp channel = (ChannelSftp) sshSession.openChannel("sftp");
        channel.connect();

        channel.put(IOUtils.toInputStream("example upload"), "thefile.txt");

        final String uploadedContents = new String(Files.readAllBytes(server.getBaseDirectory().resolve("thefile.txt")), StandardCharsets.UTF_8.name());
        assertEquals("example upload", uploadedContents);
    }
}
