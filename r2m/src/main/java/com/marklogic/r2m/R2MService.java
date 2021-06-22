package com.marklogic.r2m;

import java.io.IOException;
import java.text.ParseException;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class R2MService {

	@Async
	public void execute(R2MPayload payload) throws Exception {
		//run the R2M commandline app
		try {
			// Initialize the r2m tool
			RelationalToMarkLogic r2m = new RelationalToMarkLogic();
			r2m.setMarklogicConfiguration(payload.getMlConfig());
			r2m.setSourceConfig(payload.getSource());
			r2m.setInsertConfig(payload.getInsert());
			r2m.setTableQuery(payload.getQuery());
			r2m.run();
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
	}
}
