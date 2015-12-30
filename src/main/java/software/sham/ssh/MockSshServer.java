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

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SshServer sshServer;
    private final MockSshCommand sshCommand;

    public MockSshServer(int port) throws IOException {
        sshServer = initSshServer(port);
        sshCommand = new MockSshCommand();
        start();
    }

    public SshResponderBuilder respondTo(Matcher matcher) {
        SshResponderBuilder builder = new SshResponderBuilder();
        sshCommand.getDispatcher().add(matcher, builder.getResponder());
        return builder;
    }

    protected void start() throws IOException {
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
        sshd.setShellFactory(this);
        sshd.setCommandFactory(this);
        return sshd;
    }

    @Override
    public Command create() {
        logger.info("Creating mock SSH shell");
        return sshCommand;
    }

    @Override
    public Command createCommand(String command) {
        return create();
    }
}
