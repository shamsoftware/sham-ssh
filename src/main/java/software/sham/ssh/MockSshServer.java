package software.sham.ssh;

import com.github.fommil.ssh.SshRsaCrypto;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.keyprovider.AbstractClassLoadableResourceKeyPairProvider;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MockSshServer implements Factory<Command>, CommandFactory {
    public static final String USERNAME = "tester";
    public static final String PASSWORD = "testing";
    protected final SshServer sshServer;
    private Set<PublicKey> keys = new HashSet<PublicKey>();

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

    /**
     * @param key Key in DER format
     */
    public MockSshServer allowDerPublicKey(byte[] key) throws GeneralSecurityException {
        final KeySpec spec = new X509EncodedKeySpec(key);
        keys.add(KeyFactory.getInstance("RSA").generatePublic(spec));
        sshServer.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(this.keys));
        return this;
    }

    public MockSshServer allowOpensshPublicKey(String key) throws IOException, GeneralSecurityException {
        final SshRsaCrypto rsa = new SshRsaCrypto();
        keys.add(rsa.readPublicKey(rsa.slurpPublicKey(key)));
        sshServer.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(this.keys));
        return this;
    }

    public SshResponderBuilder respondTo(Matcher matcher) {
        SshResponderBuilder builder = new SshResponderBuilder();
        sshShell.getDispatcher().add(matcher, builder.getResponder());
        return builder;
    }

    public SshResponderBuilder respondTo(String input) {
        return respondTo(Matchers.equalTo(input));
    }

    public MockSshServer enableShell() {
        logger.info("Mock SSH shell is enabled");
        sshShell = new MockSshShell();
        setDefaults();
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
        sshd.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(this.keys));
        return sshd;
    }

    private void setDefaults() {
        respondTo("exit").withClose();
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
