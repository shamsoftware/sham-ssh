package software.sham.sftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import software.sham.ssh.MockSshServer;

public class MockSftpServer extends MockSshServer {

	private static final String MOCK_SFTP_DIRECTORY = "mock_sftp_root";

	private final Path baseDirectory;

	public MockSftpServer(int port) throws IOException {
		super(port, false);

		SftpSubsystemFactory sftpSubsystemFactory = new SftpSubsystemFactory.Builder().build();
		super.getSshServer().setSubsystemFactories(Collections.singletonList(sftpSubsystemFactory));

		this.baseDirectory = Files.createTempDirectory(MOCK_SFTP_DIRECTORY);
		log.info("baseDirectory: " + baseDirectory.toAbsolutePath().toString());
		super.getSshServer().setFileSystemFactory(new VirtualFileSystemFactory(baseDirectory.toAbsolutePath()));
	}

	@Override
	public void start() throws IOException {
		super.start();
	}

	@Override
	public void stop() throws IOException {
		super.stop();
		this.baseDirectory.toFile().delete();
	}

	public Path getBaseDirectory() {
		return baseDirectory;
	}
}
