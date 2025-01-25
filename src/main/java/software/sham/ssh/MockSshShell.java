package software.sham.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public void start(ChannelSession channel, Environment env) throws IOException {
        logger.debug("Starting mock SSH shell");
        executor.submit(eventLoop);
		
	}

	@Override
	public void destroy(ChannelSession channel) throws Exception {
		closeSession();
		executor.shutdown();
	}

	public void closeSession() {
		eventLoop.stop();
	}

	protected List<String> readInput() throws IOException {
		StringBuffer sb = new StringBuffer();
		final Charset charset = StandardCharsets.UTF_8;
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int len = in.available();
		while (len > 0) {
			if (len > 1024)
				len = 1024;
			int lenRead = in.read(buffer.array(), 0, len);
			CharBuffer cb = charset.decode(buffer);
			sb.append(cb, 0, lenRead);
			logger.trace("Read {} characters from {} bytes", cb.length(), lenRead);
			len = in.available();
		}
		
		if (sb.length() > 0)
			return Arrays.asList(sb.toString().split("\\r?\\n"));
		else
			return Collections.emptyList();
	}

	protected void writeError(Exception e) throws IOException {
		Writer writer = Channels.newWriter(Channels.newChannel(err), StandardCharsets.UTF_8.name());
		writer.write(e.toString());
		writer.flush();
		writer.close();
	}

	public void sendResponse(String output) {
		try {
			out.write(output.getBytes());
			logger.trace("Wrote output {}", output);
			out.flush();
		} catch (IOException e) {
			logger.error("Error sending response to client", e);
		}
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
			if (!this.stopped) {
				this.stopped = true;
				logger.info("Stopped Mock SSH shell event loop");
			}
		}

		@Override
		public void run() {
			while (!stopped) {
				logger.trace("Polling input...");
				try {
					List<String> input = shell.readInput();
					logger.trace("Returned from reading input");
					for (String line : input) {
						logger.debug("SSH server received input [{}]", line.toString());
						dispatcher.find(line).respond(shell);
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
			logger.debug("Event loop completed");
			callback.onExit(0);
		}
	}
}
