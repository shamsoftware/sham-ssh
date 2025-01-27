package software.sham.ssh.actions;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.sshd.server.session.ServerSession;

public class Output extends OutputSupport implements Action {
	
	private final String[] outputs;

	public Output(String... outputs) {
		super();
		
		this.outputs = outputs;
	}

	@Override
	public void respond(ServerSession serverSession, OutputStream outputStream) throws IOException {
		for (String output: outputs)
			super.write(output, outputStream);
	}


}
