package software.sham.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.sshd.git.GitLocationResolver;
import org.apache.sshd.git.pack.GitPackCommandFactory;

import software.sham.ssh.MockSshServer;

public class MockGitServer extends MockSshServer {

	private static final String MOCK_GIT_DIRECTORY = "mock_git_root";
	private Path gitRoot;

	public MockGitServer(int port) throws IOException {
		super(port, false);

		gitRoot = Files.createTempDirectory(MOCK_GIT_DIRECTORY);
		gitRoot.toFile().deleteOnExit();

		GitPackCommandFactory commandFactory = new GitPackCommandFactory()
				.withGitLocationResolver(GitLocationResolver.constantPath(gitRoot));
		super.getSshServer().setCommandFactory(commandFactory);
	}

	public Path prepareGitProject(String name) throws IOException, InterruptedException {
		Path gitProject = Files.createDirectories(gitRoot.resolve(name));

		runGit(gitProject, new String[] { "/usr/bin/git", "init" }, //
				"Couldn't initialiaze project at: " + gitProject);
		runGit(gitProject, new String[] { "/usr/bin/git", "config", "user.email", "tester@localhost" },
				"Couldn't config user.email.");
		runGit(gitProject, new String[] { "/usr/bin/git", "config", "user.name", "the tester" },
				"Couldn't config user.name.");

		Path readMeFile = gitProject.resolve("README");
		if (!Files.exists(readMeFile))
			Files.createFile(readMeFile);
		Files.writeString(readMeFile, "nothing serious");
		runGit(gitProject, new String[] { "/usr/bin/git", "add", gitProject.relativize(readMeFile).toString() }, //
				"Couldn't add README.");

		runGit(gitProject, new String[] { "/usr/bin/git", "commit", "-m", "'Initial commit.'" }, //
				"Couldn't commit.");

		return gitProject;
	}

	private void runGit(Path gitProject, String[] cmd, String errorMsg) throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(gitProject.toFile());
		pb.redirectErrorStream(true);
		Process p = pb.start();
		String processOutput = readProcessOutput(p);
		if (p.exitValue() != 0) {
			throw new RuntimeException(errorMsg + processOutput);
		}

	}

	private String readProcessOutput(Process p) throws IOException, InterruptedException {
		BufferedReader reader = p.inputReader();
		String processOutput = reader.lines().reduce("", (a, b) -> a + "\n" + b);
		return String.format("\n(returncode: %d) %s", p.waitFor(), processOutput);
	}
}
