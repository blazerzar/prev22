package prev;

import java.util.*;

import org.antlr.v4.runtime.*;

import prev.common.report.*;
import prev.phase.lexan.*;
import prev.phase.synan.*;

/**
 * The compiler.
 */
public class Compiler {

	// COMMAND LINE ARGUMENTS

	/** All valid phases of the compiler. */
	private static final String phases = "none|lexan|synan";

	/** Values of command line arguments. */
	private static HashMap<String, String> cmdLine = new HashMap<String, String>();

	/**
	 * Returns the value of a command line argument.
	 * 
	 * @param cmdLineArgName The name of the command line argument.
	 * @return The value of the specified command line argument or {@code null} if
	 *         the specified command line argument has not been used.
	 */
	public static String cmdLineArgValue(String cmdLineArgName) {
		return cmdLine.get(cmdLineArgName);
	}

	// THE COMPILER'S STARTUP METHOD

	/**
	 * The compiler's startup method.
	 * 
	 * @param args Command line arguments (see {@link prev.Compiler}).
	 */
	public static void main(String[] args) {
		try {
			Report.info("This is PREV'22 compiler:");

			// Scan the command line.
			for (int argc = 0; argc < args.length; argc++) {
				if (args[argc].startsWith("--")) {
					// Command-line switch.
					if (args[argc].matches("--src-file-name=.*")) {
						if (cmdLine.get("--src-file-name") == null) {
							cmdLine.put("--src-file-name", args[argc]);
							continue;
						}
					}
					if (args[argc].matches("--dst-file-name=.*")) {
						if (cmdLine.get("--dst-file-name") == null) {
							cmdLine.put("--dst-file-name", args[argc]);
							continue;
						}
					}
					if (args[argc].matches("--target-phase=(" + phases + "|all)")) {
						if (cmdLine.get("--target-phase") == null) {
							cmdLine.put("--target-phase", args[argc].replaceFirst("^[^=]*=", ""));
							continue;
						}
					}
					if (args[argc].matches("--logged-phase=(" + phases + "|all)")) {
						if (cmdLine.get("--logged-phase") == null) {
							cmdLine.put("--logged-phase", args[argc].replaceFirst("^[^=]*=", ""));
							continue;
						}
					}
					if (args[argc].matches("--xml=.*")) {
						if (cmdLine.get("--xml") == null) {
							cmdLine.put("--xml", args[argc].replaceFirst("^[^=]*=", ""));
							continue;
						}
					}
					if (args[argc].matches("--xsl=.*")) {
						if (cmdLine.get("--xsl") == null) {
							cmdLine.put("--xsl", args[argc].replaceFirst("^[^=]*=", ""));
							continue;
						}
					}
					Report.warning("Command line argument '" + args[argc] + "' ignored.");
				} else {
					// Source file name.
					if (cmdLine.get("--src-file-name") == null) {
						cmdLine.put("--src-file-name", args[argc]);
					} else {
						Report.warning("Source file '" + args[argc] + "' ignored.");
					}
				}
			}
			if (cmdLine.get("--src-file-name") == null) {
				throw new Report.Error("Source file not specified.");
			}
			if (cmdLine.get("--dst-file-name") == null) {
				cmdLine.put("--dst-file-name", cmdLine.get("--src-file-name").replaceFirst("\\.[^./]*$", "") + ".mms");
			}
			if ((cmdLine.get("--target-phase") == null) || (cmdLine.get("--target-phase").equals("all"))) {
				cmdLine.put("--target-phase", phases.replaceFirst("^.*\\|", ""));
			}

			// Compilation process carried out phase by phase.
			while (true) {

				// Lexical analysis.
				if (Compiler.cmdLineArgValue("--target-phase").equals("lexan"))
					try (LexAn lexan = new LexAn()) {
						while (lexan.lexer.nextToken().getType() != Token.EOF) {
						}
						break;
					}

				// Syntax analysis.
				try (LexAn lexan = new LexAn(); SynAn synan = new SynAn(lexan)) {
					SynAn.tree = synan.parser.source();
					synan.log(SynAn.tree);
				}
				if (Compiler.cmdLineArgValue("--target-phase").equals("synan"))
					break;

			}

			Report.info("Done.");
		} catch (Report.Error __) {
			System.exit(1);
		}
	}

}
