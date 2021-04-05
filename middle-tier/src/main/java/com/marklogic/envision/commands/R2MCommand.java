package com.marklogic.envision.commands;

import com.marklogic.client.ext.helper.LoggingObject;
//import com.marklogic.r2m.RelationalToMarkLogic;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class R2MCommand extends LoggingObject {
/*
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
		//	RelationalToMarkLogic r2m = new RelationalToMarkLogic();

			String joinConfigJson = new String(Files.readAllBytes(Paths.get(this.joinConfigFilePath)));
			String sourceConfigJson = new String(Files.readAllBytes(Paths.get(this.sourceConfigFilePath)));
			String insertConfigJson = new String(Files.readAllBytes(Paths.get(this.insertConfigFilePath)));
			String marklogicConfigJson = new String(Files.readAllBytes(Paths.get(this.marklogicConfigFilePath)));

			r2m.setJoinConfigJson(joinConfigJson);
			r2m.setSourceConfigJson(sourceConfigJson);
			r2m.setInsertConfigJson(insertConfigJson);
			r2m.setMarkLogicConfigJson(marklogicConfigJson);

			r2m.run();
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
	}
*/
}


