package software.sham.ssh.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

public class SshKeyPairGenerator {

	public static class GeneratorException extends Exception {

		private static final long serialVersionUID = 1L;

		public GeneratorException(Throwable cause) {
			super(cause);
		}
	}

	private final File keyFilePriv;
	private final File keyFilePub;

	public SshKeyPairGenerator(File keyFilePriv, File keyFilePub) {
		this.keyFilePriv = keyFilePriv;
		this.keyFilePub = keyFilePub;
	}

	public void generate(String privateKeyPassword) throws GeneratorException {
		try {
			generateWithJavaSecurityRsa(privateKeyPassword);
		} catch (Exception ex) {
			throw new GeneratorException(ex);
		}
	}

	private static final String JAVAX_KP_ALG = "ed25519";
	private static final String JAVAX_OPENSSH_TAG = "ssh-" + JAVAX_KP_ALG;
	private static final String JAVAX_PBE_ALG = "PBEWithSHA1AndDESede";

	/**
	 * https://stackoverflow.com/questions/41180398/how-to-add-a-password-to-an-existing-private-key-in-java
	 * 
	 * @param privateKeyPassword
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidParameterSpecException
	 */
	private void generateWithJavaSecurityRsa(String privateKeyPassword) throws NoSuchAlgorithmException, IOException,
			InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, InvalidParameterSpecException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(JAVAX_KP_ALG);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		// encrypt privateKey with password
		byte[] encodedPrivateKey = keyPair.getPrivate().getEncoded();

		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[8];
		random.nextBytes(salt);

		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 20);
		PBEKeySpec pbeKeySpec = new PBEKeySpec(privateKeyPassword.toCharArray());

		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(JAVAX_PBE_ALG);
		SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);

		Cipher cipher = Cipher.getInstance(JAVAX_PBE_ALG);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, pbeParamSpec);
		byte[] encryptePrivateKey = cipher.doFinal(encodedPrivateKey);

		AlgorithmParameters algParams = AlgorithmParameters.getInstance(JAVAX_PBE_ALG);
		algParams.init(pbeParamSpec);
		EncryptedPrivateKeyInfo encinfo = new EncryptedPrivateKeyInfo(algParams, encryptePrivateKey);

		Files.write(keyFilePriv.toPath(), encinfo.getEncoded());

		// encode publicKey in OPENSSH format
		// https://stackoverflow.com/questions/77460283/encoding-a-ed25519-public-key-to-ssh-format-in-java
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		byte[] tag = JAVAX_OPENSSH_TAG.getBytes();
		dos.writeInt(tag.length);
		dos.write(tag);

		byte[] publicKeyData = retrieveSpkiPublicKeyData(keyPair.getPublic());
		dos.writeInt(publicKeyData.length);
		dos.write(publicKeyData);

		StringBuilder publicKeySb = new StringBuilder();
		publicKeySb.append(JAVAX_OPENSSH_TAG);
		publicKeySb.append(' ');
		publicKeySb.append(Base64.getEncoder().encodeToString(baos.toByteArray()));
//		if (comment != null) {
//			publicKeySb.append(' ');
//			publicKeySb.append(comment);
//		}
		Files.writeString(keyFilePub.toPath(), publicKeySb.toString());

	}

	private byte[] retrieveSpkiPublicKeyData(PublicKey pubkey) throws IOException {
		return SubjectPublicKeyInfo.getInstance(pubkey.getEncoded()).getPublicKeyData().getOctets();
	}
}
