package com.marklogic.envision.R2MConnector;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.regex.Pattern;

import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.envision.commands.R2MCommand;
import com.marklogic.envision.hub.HubClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class R2MService extends LoggingObject{
//	protected static Pattern[] patterns = new Pattern[] {
//		Pattern.compile("&"), Pattern.compile("<"), Pattern.compile(">") };
//
//
	final private SimpMessagingTemplate template;
//	private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private static final String CONFIG_DIR = new String("/Users/frubino/projects/forks/data-hub-central-community/middle-tier/src/test/resources/r2m/config/");
	@Autowired
	R2MService(SimpMessagingTemplate template) {
		this.template = template;
	}

	@Async
	public void asyncRunConnection(HubClient client) {
		_asyncRunConnection(client);
	}

	private void _asyncRunConnection(HubClient client) {
		runR2M( client);
	}

	public void runR2M(HubClient hubClient) {
		//TODO these paths need to be passed

		String joinConfigFilePath = CONFIG_DIR + "customerConfig.json";
		String sourceConfigFilePath = CONFIG_DIR + "customerConfig.json";
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
