package software.sham.ssh;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class FunctionalTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    MockSshServer server;
    Session sshSession;
    ChannelShell sshChannel;
    WritableByteChannel inputChannel;
    ByteArrayOutputStream outputBuffer;

    @Before
    public void initSsh() throws IOException {
        server = new MockSshServer(9022);
    }

    @Before
    public void initSshClient() throws JSchException, IOException {
        JSch jsch = new JSch();
        sshSession = jsch.getSession("tester", "localhost", 9022);
        Properties config = new Properties();
        config.setProperty("StrictHostKeyChecking", "no");
        sshSession.setConfig(config);
        sshSession.setPassword("testing");
        sshSession.connect();
        sshChannel = (ChannelShell) sshSession.openChannel("shell");
        PipedInputStream channelIn = new PipedInputStream();
        sshChannel.setInputStream(channelIn);
        inputChannel = Channels.newChannel(new PipedOutputStream(channelIn));
        outputBuffer = new ByteArrayOutputStream();
        sshChannel.setOutputStream(outputBuffer);
        sshChannel.connect(1000);
    }

    @After
    public void stopSsh() throws IOException {
        sshSession.disconnect();
        server.stop();
    }

    @Test
    public void defaultShellCommandsShouldSilentlySucceed() throws Exception {
        sendTextToServer("Knock knock\n");
        Thread.sleep(200);
        assertThat(outputBuffer.size(), equalTo(0));
    }

    @Test
    public void singleOutput() throws Exception {
        server.respondTo(any(String.class))
            .withOutput("hodor\n");

        sendTextToServer("Knock knock\n");
        Thread.sleep(200);
        assertEquals("hodor\n", outputBuffer.toString());
    }

    @Test
    public void multipleOutput() throws Exception {
        server.respondTo(any(String.class))
                .withOutput("Starting...\n")
                .withOutput("Completed.\n");

        sendTextToServer("start");
        Thread.sleep(200);
        assertEquals("Starting...\nCompleted.\n", outputBuffer.toString());
    }

    @Test
    public void delayedOutput() throws Exception {
        server.respondTo(any(String.class))
                .withOutput("Starting...\n")
                .withDelay(500)
                .withOutput("Completed.\n");

        sendTextToServer("start");
        Thread.sleep(50);
        logger.debug("Checking for first line");
        assertEquals("Starting...\n", outputBuffer.toString());
        Thread.sleep(550);
        logger.debug("Checking for second line");
        assertEquals("Starting...\nCompleted.\n", outputBuffer.toString());
    }

    private void sendTextToServer(String text) throws IOException {
        try (Writer writer = Channels.newWriter(inputChannel, StandardCharsets.UTF_8.name())) {
            writer.write(text);
        }
        logger.debug("Sent input to SSH server: {}", text);
    }
}
