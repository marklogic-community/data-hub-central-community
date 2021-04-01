package com.marklogic.r2m;

import org.apache.commons.cli.*;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws Exception {
		System.out.println("r2m rocks!");

		// Pull the config file paths from the arguments and parse the JSON files
		Options options = new Options();

		Option joinOpt = new Option("jc", "joinCfg", true, "Join Config File Path");
		joinOpt.setRequired(true);
		options.addOption(joinOpt);

		Option sourceCfgOpt = new Option("sc", "sourceConfig", true, "Source database config file path");
		sourceCfgOpt.setRequired(true);
		options.addOption(sourceCfgOpt);

		Option miCfgOpt = new Option("mi", "insertConfig", true, "MarkLogic insert config file path");
		miCfgOpt.setRequired(true);
		options.addOption(miCfgOpt);

		Option mlcfgOpt = new Option("mc", "mlConfig", true, "MarkLogic Config File Path");
		mlcfgOpt.setRequired(true);
		options.addOption(mlcfgOpt);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		try {
			CommandLine cmd = parser.parse(options, args);

			String joinConfigFilePath = cmd.getOptionValue("joinCfg");
			String sourceConfigFilePath = cmd.getOptionValue("sourceConfig");
			String insertConfigFilePath = cmd.getOptionValue("insertConfig");
			String marklogicConfigFilePath = cmd.getOptionValue("mlConfig");

			String joinConfigJson = new String(Files.readAllBytes(Paths.get(joinConfigFilePath)));
			String sourceConfigJson = new String(Files.readAllBytes(Paths.get(sourceConfigFilePath)));
			String insertConfigJson = new String(Files.readAllBytes(Paths.get(insertConfigFilePath)));
			String marklogicConfigJson = new String(Files.readAllBytes(Paths.get(marklogicConfigFilePath)));

			// Initialize the r2m tool
			RelationalToMarkLogic r2m = new RelationalToMarkLogic();

			r2m.setJoinConfigJson(joinConfigJson);
			r2m.setSourceConfigJson(sourceConfigJson);
			r2m.setInsertConfigJson(insertConfigJson);
			r2m.setMarkLogicConfigJson(marklogicConfigJson);

			r2m.run();
		} catch (ParseException e) {
			formatter.printHelp("r2m", options);
			e.printStackTrace();
		}
	}
}

