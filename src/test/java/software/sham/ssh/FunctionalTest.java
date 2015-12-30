package software.sham.ssh;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class FunctionalTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    MockSshServer server;
    Session sshSession;
    ChannelShell sshChannel;
    Writer input;
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
        input = Channels.newWriter(
                Channels.newChannel(new PipedOutputStream(channelIn)), StandardCharsets.UTF_8.name());
        outputBuffer = new ByteArrayOutputStream();
        sshChannel.setOutputStream(outputBuffer);
        sshChannel.connect(2000);
    }

    @After
    public void stopSsh() throws IOException {
        sshSession.disconnect();
        server.stop();
    }

    @Test
    public void defaultShellCommandsShouldSilentlySucceed() throws Exception {
        input.write("Knock knock\n");
        Thread.sleep(500);
        assertThat(outputBuffer.size(), equalTo(0));
    }

    @Test
    public void simpleStubShouldRespond() throws Exception {
        server.respondTo(any(String.class))
            .withText("hodor\n");

        Thread.sleep(300);
        input.write("Knock knock\n");
        input.flush();
        input.close();
        logger.debug("sent some stuff");
        Thread.sleep(500);
        assertEquals("hodor\n", outputBuffer.toString());
        logger.info("Well my assertions passed");
    }
}
