package software.sham.ssh.actions;

import software.sham.ssh.MockSshShell;

import java.io.IOException;

public interface Action {
    void respond(MockSshShell shell) throws IOException;
}
