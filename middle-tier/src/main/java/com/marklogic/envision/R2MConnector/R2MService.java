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
	protected Random random;
	protected long randomValue;
	protected long milliSecs;
	protected static Pattern[] patterns = new Pattern[] {
		Pattern.compile("&"), Pattern.compile("<"), Pattern.compile(">") };


	final private SimpMessagingTemplate template;
	private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	@Autowired
	R2MService(SimpMessagingTemplate template) {
		this.template = template;
		random = new Random();
		randomValue = random.nextLong();
		Calendar cal = Calendar.getInstance();
		milliSecs = cal.getTimeInMillis();
	}

	@Async
	public void asyncRunConnection(HubClient client) {
		_asyncRunConnection(client);
	}

	private void _asyncRunConnection(HubClient client) {
		runR2M( client);
	}

	public void runR2M(HubClient hubClient) {
		String joinConfigFilePath = "";
		String sourceConfigFilePath = "";
		String insertConfigFilePath = "";
		String marklogicConfigFilePath = "";
		try {
/*			new R2MCommand(
				joinConfigFilePath,
				sourceConfigFilePath,
				insertConfigFilePath,
				marklogicConfigFilePath ).execute();

 */
		}
		catch (Error | Exception error) {
			error.printStackTrace();
		}
	}
}
