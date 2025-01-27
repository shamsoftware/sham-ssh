package software.sham.ssh.actions;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class OutputSupport {
	protected static final byte[] EOL = System.lineSeparator().getBytes();
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	void write(String output, OutputStream outputStream) throws IOException {
		logger.debug("Sending output: " + output);
		outputStream.write(output.getBytes());
		outputStream.write(EOL);
	}
}
