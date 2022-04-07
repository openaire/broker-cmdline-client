package eu.dnetlib.broker;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BrokerClientApp implements CommandLineRunner {

	private static Logger log = LoggerFactory.getLogger(BrokerClientApp.class);

	private static final String APPLICATION_JAR = "./broker-cmdline-client.jar";
	private static final String APPLICATION_TITLE = "OpenAIRE Broker - Public API Client";
	private static final String APPLICATION_FOOTER = "\nSee http://api.openaire.eu/broker for further details.\n";

	@Value("${dhp.broker.api.base-url}")
	private String defaultBrokerApiBaseUrl;

	@Autowired
    private BrokerClient brokerClient;

	private final static CommandLineParser cmdLineParser = new DefaultParser();

	private final static Options options = new Options()
		.addOption(Option.builder("u")
			.required(true)
			.hasArg(true)
			.longOpt("user")
			.desc("the email of the subscriber (REQUIRED)")
			.build())
		.addOption(Option.builder("bu")
			.required(false)
			.hasArg(true)
			.longOpt("baseurl")
			.desc("override of the default Broker Public Api baseUrl")
			.build())
		.addOption(Option.builder("o")
			.required(true)
			.hasArg(true)
			.longOpt("output")
			.desc("the output directory (REQUIRED)")
			.build())
		.addOption(Option.builder("z")
			.required(false)
			.hasArg(false)
			.desc("compress the output files in GZIP format")
			.build())
		.addOption(Option.builder("i")
			.required(false)
			.hasArg(false)
			.desc("interactive mode")
			.build())
		.addOption(Option.builder("h")
			.longOpt("help")
			.required(false)
			.hasArg(false)
			.desc("help")
			.build())
		.addOption(Option.builder("v")
			.required(false)
			.hasArg(false)
			.desc("verbose")
			.build())
		.addOption(Option.builder("vv")
			.required(false)
			.hasArg(false)
			.desc("show debug logs")
			.build());

	public static void main(final String[] args) {

		// TO AVOID EXCEPTIONS WITH MANDATORY FIELDS
		for (final String s : args) {
			if (s.equals("-h") || s.equals("--help")) {
				printHelpAndExit(options);
			}
		}

		try {

			final CommandLine cmd = cmdLineParser.parse(options, args, false);

			if (cmd.hasOption("v")) {
				SpringApplication.run(BrokerClientApp.class, ArrayUtils.add(args, "--logging.level.root=INFO"));
			} else if (cmd.hasOption("vv")) {
				SpringApplication.run(BrokerClientApp.class, ArrayUtils.add(args, "--logging.level.root=DEBUG"));
			} else {
				SpringApplication.run(BrokerClientApp.class, args);
			}
		} catch (final ParseException e) {
			System.err.println("\nERROR: " + e.getMessage());
			printHelpAndExit(options);
		}
	}

	@Override
	public void run(final String... args) throws Exception {
		System.out.println();

		log.info(String.format("**** EXECUTING - %s ***", APPLICATION_TITLE));

		final CommandLine cmd = cmdLineParser.parse(options, args, true);

		final String user = cmd.getOptionValue("u");
		final URL baseUrl = new URL(cmd.getOptionValue("bu", defaultBrokerApiBaseUrl));
		final File outputDir = prepareDir(cmd.getOptionValue("o"));
		final boolean gzip = cmd.hasOption("z");
		final boolean interactive = cmd.hasOption("i");

		log.info("* PARAMS: USER: " + user);
		log.info("* PARAMS: BASE_URL: " + baseUrl);
		log.info("* PARAMS: OUTPUT DIR: " + outputDir);
		log.info("* PARAMS: SAVE AS GZIP: " + gzip);
		log.info("* PARAMS: INTERACTIVE MODE: " + interactive);

        for (final String s : brokerClient.listSubscriptions(baseUrl, user)) {
			if (!interactive || confirm(s)) {
                brokerClient.downloadEvents(baseUrl, s, outputDir, gzip);
			} else {
				System.out.println("-- SKIPPED --");
			}
		}

		log.info("**** DONE ***");
		System.out.println();

	}

	private boolean confirm(final String s) throws IOException {
		System.out.print(String.format("\nDo you want download subscription %s? (Y/N) ", s));
		System.out.flush();

		char c = (char) System.in.read();
		while (true) {
			if (c == 'y' || c == 'Y') {
				return true;
			} else if (c == 'n' || c == 'N') {
				return false;
			} else {
				c = (char) System.in.read();
			}
		}
	}

	private File prepareDir(final String path) {
		final File dir = new File(path);

		if (dir.exists() && dir.isDirectory()) {
			log.info("Reusing existent directory: " + path);
			return dir;

		}

		if (!dir.exists() && dir.mkdirs()) {
			log.info("New directory created: " + path);
			return dir;
		}

		log.error("Invalid directory: " + path);
		throw new RuntimeException("Invalid directory: " + path);
	}

	private static void printHelpAndExit(final Options options) {
		final String ln = StringUtils.repeat("=", APPLICATION_TITLE.length());
		System.out.println(String.format("\n%s\n%s\n%s\n", ln, APPLICATION_TITLE, ln));
		new HelpFormatter().printHelp(APPLICATION_JAR, options, true);
		System.out.println(APPLICATION_FOOTER);

		System.exit(1);
	}
}
