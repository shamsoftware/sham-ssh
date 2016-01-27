package software.sham.ssh.actions;

import software.sham.ssh.MockSshShell;

public class Close implements Action {
    @Override
    public void respond(MockSshShell shell) {
        shell.closeSession();
    }
}
