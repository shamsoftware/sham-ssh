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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class MockSftpServer {
    public static final String USERNAME = "tester";
    public static final String PASSWORD = "testing";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SshServer sshServer;

    private Path baseDirectory;
    private Path workDirectory;

    public MockSftpServer(int port) throws IOException {
        sshServer = initSftpServer(port);
        start();
    }

    public Path getBaseDirectory() {
        return baseDirectory;
    }

    protected void start() throws IOException {
        baseDirectory = Files.createTempDirectory("sftproot");
        workDirectory = Files.createTempDirectory("tempsftp-work");

        logger.info("workDirectory: "+workDirectory);
        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(baseDirectory.toAbsolutePath().toString()));

        AbstractClassLoadableResourceKeyPairProvider keyPairProvider = SecurityUtils.createClassLoadableResourceKeyPairProvider();
        keyPairProvider.setResources(Arrays.asList("keys/sham-sftp-id-dsa"));
        sshServer.setKeyPairProvider(keyPairProvider);

        sshServer.start();
    }

    public void stop() throws IOException {
        sshServer.stop();
        FileUtils.deleteQuietly(baseDirectory.toFile());
        FileUtils.deleteQuietly(workDirectory.toFile());
    }

    private SshServer initSftpServer(int port) {
        final SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystemFactory()));
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                return USERNAME.equals(username) && PASSWORD.equals(password);
            }

        });
        return sshd;
    }
}
