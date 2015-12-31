package software.sham.ssh;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MockSshShell implements Command {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private final ResponderDispatcher dispatcher = new ResponderDispatcher();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private MockShellEventLoop eventLoop = new MockShellEventLoop(this);

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
        logger.debug("Starting mock SSH shell");
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

    public ResponderDispatcher getDispatcher() {
        return this.dispatcher;
    }

    public class MockShellEventLoop implements Runnable {
        private boolean stopped = false;
        private final MockSshShell shell;
        public MockShellEventLoop(MockSshShell shell) {
            this.shell = shell;
        }

        public void stop() {
            this.stopped = true;
            logger.info("Stopped Mock SSH shell event loop");
        }

        @Override
        public void run() {
            while(! stopped) {
                logger.trace("Polling input...");
                try {
                    List<String> input = shell.readInput();
                    for (String line : input) {
                        logger.debug("Found input " + line.toString());
                        dispatcher.find(line).respond(out);
                    }
                    Thread.sleep(100);
                } catch (IOException e) {
                    try {
                        shell.writeError(e);
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
