//package software.sham.sftp;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Collections;
//
//import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
//import org.apache.sshd.sftp.server.SftpSubsystemFactory;
//
//import software.sham.ssh.MockSshServer;
//
//public class MockSftpServer extends MockSshServer {
//
//    private Path baseDirectory;
//
//    public MockSftpServer(int port) throws IOException {
//        this(port, false);
//    }
//
//    private MockSftpServer(int port, boolean enableShell) throws IOException {
//        super(port, false);
//        initSftp();
//        if (enableShell) {
//            enableShell();
//        }
//        start();
//    }
//
//    private void initSftp() {
//    	SftpSubsystemFactory sftpSubsystemFactory = new SftpSubsystemFactory.Builder().build();
//        sshServer.setSubsystemFactories(Collections.singletonList(sftpSubsystemFactory));               
//    }
//
//    public Path getBaseDirectory() {
//        return baseDirectory;
//    }
//
//    @Override
//    public void start() throws IOException {
//        baseDirectory = Files.createTempDirectory("sftproot");
//        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(baseDirectory.toAbsolutePath()));
//        super.start();
//    }
//
//    @Override
//    public void stop() throws IOException {
//        super.stop();
//        baseDirectory.toFile().delete();
//    }
//
//    public static MockSftpServer createWithShell(int port) throws IOException {
//        return new MockSftpServer(port, true);
//    }
//}
