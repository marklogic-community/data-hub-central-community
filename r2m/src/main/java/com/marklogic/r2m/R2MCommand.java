package com.marklogic.r2m;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class R2MCommand  {

	private ObjectMapper objectMapper = new ObjectMapper();

	private final String joinConfigFilePath;
	private final String sourceConfigFilePath;
	private final String insertConfigFilePath;
	private final String marklogicConfigFilePath;

	public R2MCommand(String joinConfigFilePath, String sourceConfigFilePath, String insertConfigFilePath, String marklogicConfigFilePath) {
		this.joinConfigFilePath = joinConfigFilePath;
		this.sourceConfigFilePath = sourceConfigFilePath;
		this.insertConfigFilePath = insertConfigFilePath;
		this.marklogicConfigFilePath = marklogicConfigFilePath;
	}

	public void execute() throws Exception {
		//run the R2M commandline app
		try {
			// Initialize the r2m tool
			RelationalToMarkLogic r2m = new RelationalToMarkLogic();

			TableQuery tableQuery;
			SourceConfiguration sourceConfig;
			MarkLogicConfiguration marklogicConfiguration;
			MLInsertConfig insertConfig;

			String joinConfigJson = new String(Files.readAllBytes(Paths.get(this.joinConfigFilePath)));
			String sourceConfigJson = new String(Files.readAllBytes(Paths.get(this.sourceConfigFilePath)));
			String insertConfigJson = new String(Files.readAllBytes(Paths.get(this.insertConfigFilePath)));
			String marklogicConfigJson = new String(Files.readAllBytes(Paths.get(this.marklogicConfigFilePath)));

			// Initialize config JSON files
			try {
				tableQuery = objectMapper.readerFor(TableQuery.class).readValue(joinConfigJson);
				r2m.setTableQuery(tableQuery);
			} catch (IOException e) {
				throw new Exception("Unable to read join configuration JSON: " + joinConfigJson, e);
			}

			try {
				sourceConfig = objectMapper.readerFor(SourceConfiguration.class).readValue(sourceConfigJson);
				r2m.setSourceConfig(sourceConfig);
			} catch (IOException e) {
				throw new Exception("Unable to read MarkLogic insert configuration JSON: " + sourceConfigJson, e);
			}

			try {
				marklogicConfiguration = objectMapper.readerFor(MarkLogicConfiguration.class).readValue(marklogicConfigJson);
				r2m.setMarklogicConfiguration(marklogicConfiguration);
			} catch (IOException e) {
				throw new Exception("Unable to read MarkLogic configuration JSON: " + marklogicConfigJson, e);
			}

			try {
				insertConfig = objectMapper.readerFor(MLInsertConfig.class).readValue(insertConfigJson);
				r2m.setInsertConfig(insertConfig);
			} catch (IOException e) {
				throw new Exception("Unable to read insert configuration JSON: " + insertConfigJson, e);
			}

			r2m.run();
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
	}

}


