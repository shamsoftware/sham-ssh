package software.sham.ssh;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class PublicKeyRetriever {
	private final X509EncodedKeySpec keySpec;

	public PublicKeyRetriever(X509EncodedKeySpec keySpec) {
		this.keySpec = keySpec;
	}

	public PublicKey getPublicKey() throws GeneralSecurityException {
		switch (keySpec.getAlgorithm()) {
		case "OPENSSH":
			return parseSshEnvelopeBc(keySpec);
		case "SSH2":
		case "PEM":
			return parsePemEnvelope(keySpec);
		default:
			return KeyFactory.getInstance(keySpec.getAlgorithm()).generatePublic(keySpec);
		}
	}

	private PublicKey parseSshEnvelopeBc(X509EncodedKeySpec keySpec) throws GeneralSecurityException {
		try {
			// decompose OPENSSH format to its essence
			String base64pubKey = new String(keySpec.getEncoded()) //
					.split(" ")[1] //
					.replaceAll(System.lineSeparator(), "");
			byte[] pubKey = Base64.getDecoder().decode(base64pubKey);

			// parse & convert
			AsymmetricKeyParameter keyParams = OpenSSHPublicKeyUtil.parsePublicKey(pubKey);
			SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keyParams);
			return new JcaPEMKeyConverter().getPublicKey(publicKeyInfo);
		} catch (IOException ex) {
			throw new GeneralSecurityException(ex);
		}

	}

	/**
	 * https://www.baeldung.com/java-read-pem-file-keys
	 * 
	 * @param keySpec
	 * @return
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	private PublicKey parsePemEnvelope(X509EncodedKeySpec keySpec)
			throws InvalidKeySpecException, NoSuchAlgorithmException {
		String publicKeyPem = new String(keySpec.getEncoded()) //
				.replace("-----BEGIN PUBLIC KEY-----", "") //
				.replaceAll(System.lineSeparator(), "") //
				.replace("-----END PUBLIC KEY-----", "");

		byte[] encoded = Base64.getDecoder().decode(publicKeyPem.getBytes());

		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
	}
}