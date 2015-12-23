package software.sham.sftp;

import static org.junit.Assert.*;

import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class FunctionalTest {
    MockSftpServer server;

    @Before
    public void initSftp() throws IOException {
        server = new MockSftpServer(9022);
    }

    @After
    public void stopSftp() throws IOException {
        server.stop();
    }

    @Test
    public void connectAndDownloadFile() throws JSchException, IOException, SftpException {
        Files.copy(IOUtils.toInputStream("example file contents"), server.getBaseDirectory().resolve("example.txt"));

        JSch jsch = new JSch();
        Session session = jsch.getSession("tester", "localhost", 9022);
        Properties config = new Properties();
        config.setProperty("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setPassword("testing");
        session.connect();

        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        final String downloadedContents = IOUtils.toString(channel.get("example.txt"));
        assertEquals("example file contents", downloadedContents);
    }
}
