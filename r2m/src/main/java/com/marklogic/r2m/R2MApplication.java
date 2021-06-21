package com.marklogic.r2m;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class R2MApplication
	implements CommandLineRunner {

	private static final String CONFIG_DIR = new String("/Users/frubino/projects/forks/data-hub-central-community/middle-tier/src/test/resources/r2m/config/");

	private static Logger LOG = LoggerFactory
		.getLogger(R2MApplication.class);

	public static void main(String[] args) {
		LOG.info("STARTING THE APPLICATION");
		SpringApplication.run(R2MApplication.class, args);
		LOG.info("APPLICATION FINISHED");
	}

	@Override
	public void run(String... args) {
		runR2M();
	}

	public void runR2M() {
		//TODO these paths need to be passed

		String joinConfigFilePath = CONFIG_DIR + "customerConfig.json";
		String sourceConfigFilePath = CONFIG_DIR + "sourceConfig.json";
		String insertConfigFilePath = CONFIG_DIR + "customerInsertConfig.json";
		String marklogicConfigFilePath = CONFIG_DIR + "marklogicConfiguration.json";


		try {
			R2MCommand r2mCmd =
				new R2MCommand(
					joinConfigFilePath,
					sourceConfigFilePath,
					insertConfigFilePath,
					marklogicConfigFilePath );

			r2mCmd.execute();


		}
		catch (Error | Exception error) {
			error.printStackTrace();
		}
	}
}
