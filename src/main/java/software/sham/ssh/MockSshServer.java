package software.sham.ssh;

import org.apache.sshd.common.Factory;
import org.apache.sshd.common.keyprovider.AbstractClassLoadableResourceKeyPairProvider;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class MockSshServer implements Factory<Command>, CommandFactory {
    public static final String USERNAME = "tester";
    public static final String PASSWORD = "testing";
    protected final SshServer sshServer;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private MockSshShell sshShell;

    public MockSshServer(int port) throws IOException {
        this(port, true);
    }

    protected MockSshServer(int port, boolean shouldStartServices) throws IOException {
        sshServer = initSshServer(port);
        if (shouldStartServices) {
            enableShell();
            start();
        }
    }

    public SshResponderBuilder respondTo(Matcher matcher) {
        SshResponderBuilder builder = new SshResponderBuilder();
        sshShell.getDispatcher().add(matcher, builder.getResponder());
        return builder;
    }

    public MockSshServer enableShell() {
        logger.info("Mock SSH shell is enabled");
        sshShell = new MockSshShell();
        sshServer.setShellFactory(this);
        return this;
    }

    public void start() throws IOException {
        AbstractClassLoadableResourceKeyPairProvider keyPairProvider = SecurityUtils.createClassLoadableResourceKeyPairProvider();
        keyPairProvider.setResources(Arrays.asList("keys/sham-ssh-id-dsa"));
        sshServer.setKeyPairProvider(keyPairProvider);

        sshServer.start();
    }

    public void stop() throws IOException {
        sshServer.stop();
    }

    protected SshServer initSshServer(int port) {
        final SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                return USERNAME.equals(username) && PASSWORD.equals(password);
            }

        });
        return sshd;
    }

    @Override
    public Command create() {
        logger.debug("Creating mock SSH shell");
        return sshShell;
    }

    @Override
    public Command createCommand(String command) {
        return create();
    }
}
