package software.sham.ssh.actions;

import java.io.IOException;

import org.apache.sshd.common.SshConstants;
import org.apache.sshd.server.session.ServerSession;

public class Close implements Action {
	@Override
	public void respond(ServerSession serverSession) throws IOException {
		serverSession.disconnect(SshConstants.SSH2_DISCONNECT_BY_APPLICATION, "mock close");
	}
}
