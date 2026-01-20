package software.sham.ssh;

import java.util.ArrayList;

import org.apache.sshd.common.util.threads.CloseableExecutorService;
import org.apache.sshd.server.command.AbstractCommandSupport;

abstract class CommandParserSupport extends AbstractCommandSupport {

	private static final String COMMAND_SEPERATOR = ";";
	private static final String COMMAND_QUOTES = "\"";

	public CommandParserSupport(String command, CloseableExecutorService executorService) {
		super(command, executorService);
	}

	String[] divideCommands(String line) {
		String[] frags = line.split(CommandParserSupport.COMMAND_SEPERATOR);

		ArrayList<String> commands = new ArrayList<String>();

		int i = 0;
		while (i < frags.length) {
			String frag = frags[i];

			while ((frag.length() - frag.replace(CommandParserSupport.COMMAND_QUOTES, "").length()) % 2 == 1) {
				if (i + 1 < frags.length) {
					frag += CommandParserSupport.COMMAND_SEPERATOR;
					frag += frags[++i];
				} else {
					log.error("command division met unbalanced quote situation in: " + line);
					return new String[] { line };
				}
			}

			commands.add(frag);
			i++;
		}

		return commands.toArray(new String[0]);
	}

}