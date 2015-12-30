package software.sham.sftp;

import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.AbstractClassLoadableResourceKeyPairProvider;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.sham.ssh.MockSshServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class MockSftpServer extends MockSshServer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Path baseDirectory;

    public MockSftpServer(int port) throws IOException {
        super(port, false);
        initSftp();
        start();
    }

    private void initSftp() {
        sshServer.setCommandFactory(new ScpCommandFactory());
        sshServer.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystemFactory()));
    }

    public Path getBaseDirectory() {
        return baseDirectory;
    }

    @Override
    public void start() throws IOException {
        baseDirectory = Files.createTempDirectory("sftproot");
        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(baseDirectory.toAbsolutePath().toString()));
        super.start();
    }

    @Override
    public void stop() throws IOException {
        super.stop();
        FileUtils.deleteQuietly(baseDirectory.toFile());
    }

}
