package com.marklogic.envision.R2MConnector;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Random;

import com.marklogic.envision.pojo.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.marklogic.r2m.*;

@Service
public class R2MService {

	final private SimpMessagingTemplate template;

	@Autowired
	R2MService(SimpMessagingTemplate template) {
		this.template = template;
	}

	@Async
	public void execute(R2MPayload payload) throws Exception {
		StatusMessage msg = StatusMessage.newStatus("Loading from RDBMS");
		//run the R2M commandline app
		try {
			// Initialize the r2m tool
			RelationalToMarkLogic r2m = new RelationalToMarkLogic(template);
			r2m.setMarklogicConfiguration(payload.getMlConfig());
			r2m.setSourceConfig(payload.getSource());
			r2m.setInsertConfig(payload.getInsert());
			r2m.setTableQuery(payload.getQuery());
			updateStatus(msg.withMessage("Running load..."));
			r2m.run();
		} catch (ParseException | IOException e) {
			updateStatus(msg.withError(e.getMessage()));
			e.printStackTrace();
		}
	}
	private void updateStatus(StatusMessage message) {
		template.convertAndSend("/topic/status", message);
	}
}
