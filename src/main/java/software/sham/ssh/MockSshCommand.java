package software.sham.ssh;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MockSshCommand implements Command {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private final ResponderDispatcher dispatcher = new ResponderDispatcher();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private MockCommandEventLoop eventLoop = new MockCommandEventLoop(this);

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(Environment env) throws IOException {
        logger.debug("Starting mock SSH command");
        executor.submit(eventLoop);
    }

    @Override
    public void destroy() {
        eventLoop.stop();
        callback.onExit(0);
        executor.shutdown();
    }

    protected List<String> readInput() throws IOException {
        BufferedReader reader = new BufferedReader(Channels.newReader(Channels.newChannel(in), StandardCharsets.UTF_8.name()));
        return IOUtils.readLines(reader);
    }

    protected void writeError(Exception e) throws IOException {
        Writer writer = Channels.newWriter(Channels.newChannel(err), StandardCharsets.UTF_8.name());
        writer.write(e.toString());
        writer.flush();
        writer.close();
    }

    protected void writeOutput(String output) throws IOException {
        logger.trace("Writing output " + output + " to stream " + out.toString());
        Writer writer = new BufferedWriter(Channels.newWriter(Channels.newChannel(out), StandardCharsets.UTF_8.name()));
        writer.write(output);
        writer.flush();
        writer.close();
    }

    public ResponderDispatcher getDispatcher() {
        return this.dispatcher;
    }

    public class MockCommandEventLoop implements Runnable {
        private boolean stopped = false;
        private final MockSshCommand command;
        public MockCommandEventLoop(MockSshCommand command) {
            this.command = command;
        }

        public void stop() {
            this.stopped = true;
            logger.info("Stopped Mock SSH command event loop");
        }

        @Override
        public void run() {
            while(! stopped) {
                try {
                    logger.trace("Polling input...");
                    List<String> input = command.readInput();
                    for (String line : input) {
                        logger.debug("Found input " + line.toString());
                        command.writeOutput(dispatcher.find(line).respond());
                    }
                    Thread.sleep(250);
                } catch (IOException e) {
                    try {
                        command.writeError(e);
                    } catch (IOException e2) {
                        System.err.println(e2.toString());
                    }
                } catch (InterruptedException e) {
                    logger.debug("Interrupted event loop thread: " + e.getMessage());
                }
            }
            callback.onExit(0);
        }
    }
}
