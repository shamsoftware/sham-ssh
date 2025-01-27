package software.sham.ssh;

import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.junit.Test;

import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class MockSshServerConnectionTest extends MockSshServerTestSupport {

	@Test(expected = IOException.class)
	public void shouldDenyUnknownKey() throws IOException, InvalidPassphraseException, SshException {
		// unknown to server
		SshKeyPair keyPair = SshKeyUtils.getPrivateKey(Thread.currentThread().getContextClassLoader() //
				.getResourceAsStream("keys/id_rsa_tester"), MockSshServerTestSupport.SSH_SERVER_USER_KEY_PASSPHRASE);

		initSshClientWithKey(keyPair); // should throw IOException: Authentication failed
	}

	@Test
	public void shouldSupportDERPublicKeyAuth() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			InvalidPassphraseException, SshException {
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(
				Thread.currentThread().getContextClassLoader().getResourceAsStream("keys/id_rsa_tester.der.pub"))) {

			PublicKey publicKey = KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(bufferedInputStream.readAllBytes()));
			testPublicKeyAuth(publicKey);
		}
	}

	@Test
	public void shouldSupportSSHPublicKeyAuth() throws IOException, InvalidPassphraseException, SshException {
		SshPublicKey publicKey = SshKeyUtils.getPublicKey(Thread.currentThread().getContextClassLoader() //
				.getResourceAsStream("keys/id_rsa_tester.pub"));

		testPublicKeyAuth(publicKey.getJCEPublicKey());
	}

	@Test
	public void shouldSupportPKCS8PublicKeyAuth()
			throws IOException, URISyntaxException, SshException, InvalidPassphraseException {
		try (PEMParser parser = new PEMParser(new InputStreamReader(
				Thread.currentThread().getContextClassLoader().getResourceAsStream("keys/id_rsa_tester.pkcs8.pub")))) {
			SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(parser.readObject());
			PublicKey publicKey = new JcaPEMKeyConverter().getPublicKey(subjectPublicKeyInfo);
			testPublicKeyAuth(publicKey);
		}
	}

	@Test
	public void shouldSupportGeneratorPublicKeyAuth() throws IOException, SshException, InvalidPassphraseException {
		final String privKeyPassword = "generated_testing";

		File privKey = File.createTempFile("privKey", null);
		Files.setPosixFilePermissions(privKey.toPath(), PosixFilePermissions.fromString("rw-" + "------"));
		privKey.deleteOnExit();
		File pubKey = File.createTempFile("pubKey", null);
		pubKey.deleteOnExit();

		SshKeyPair keyPair = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.SSH2_RSA);

		SshKeyUtils.savePrivateKey(keyPair, privKeyPassword, null, privKey);
		SshKeyUtils.createPublicKeyFile(keyPair.getPublicKey(), null, pubKey, SshPublicKeyFileFactory.OPENSSH_FORMAT);

		SshKeyPair sshKeyPair = SshKeyUtils.getPrivateKey(privKey, privKeyPassword);
		SshPublicKey publicKey = SshKeyUtils.getPublicKey(pubKey);

		testPublicKeyAuth(sshKeyPair, publicKey.getJCEPublicKey());
	}

	private void initSshClientWithKey(SshKeyPair sshKeyPair) throws IOException, SshException {

		this.sshClient = SshClientBuilder.create().withIdentities(sshKeyPair)
				.withUsername(MockSshServerTestSupport.SSH_SERVER_USER)
				.withTarget(MockSshServerTestSupport.SSH_SERVER_HOSTNAME, MockSshServerTestSupport.SSH_SERVER_PORT)
				.build();

		this.sshClient.openSessionChannel(MockSshServerTestSupport.SSH_SERVER_CONNECT_TIMOUT, true);
	}

	private void testPublicKeyAuth(PublicKey publicKey) throws IOException, InvalidPassphraseException, SshException {
		SshKeyPair keyPair = SshKeyUtils.getPrivateKey(Thread.currentThread().getContextClassLoader() //
				.getResourceAsStream("keys/id_rsa_tester"), MockSshServerTestSupport.SSH_SERVER_USER_KEY_PASSPHRASE);

		testPublicKeyAuth(keyPair, publicKey);
	}

	private void testPublicKeyAuth(SshKeyPair keyPair, PublicKey publicKey) throws IOException, SshException {
		server.allowPublicKey(publicKey);

		initSshClientWithKey(keyPair);

		assertTrue(this.sshClient.isAuthenticated());
	}

}
