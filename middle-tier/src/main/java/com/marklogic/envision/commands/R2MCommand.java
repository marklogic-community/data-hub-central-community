package com.marklogic.envision.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.client.ext.util.DefaultDocumentPermissionsParser;
import com.marklogic.client.ext.util.DocumentPermissionsParser;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.mgmt.util.ObjectMapperFactory;

import java.io.IOException;
import java.nio.file.Path;

public class R2MCommand extends LoggingObject {

	private final HubClient hubClient;
	private final String username;

	private final DocumentPermissionsParser documentPermissionsParser = new DefaultDocumentPermissionsParser();
	private final ObjectMapper objectMapper;


	public R2MCommand(HubClient hubClient, String username) {
		this.hubClient = hubClient;
		this.username = username;
		this.objectMapper = ObjectMapperFactory.getObjectMapper();
	}

	public void execute() {
		DatabaseClient stagingClient = hubClient.getStagingClient();
		DatabaseClient finalClient = hubClient.getFinalClient();

		Path entitiesPath = hubClient.getHubConfig().getHubEntitiesDir();

		JSONDocumentManager finalDocMgr = finalClient.newJSONDocumentManager();
		JSONDocumentManager stagingDocMgr = stagingClient.newJSONDocumentManager();

		DocumentWriteSet finalEntityDocumentWriteSet = finalDocMgr.newWriteSet();
		DocumentWriteSet stagingEntityDocumentWriteSet = stagingDocMgr.newWriteSet();
		//run the R2M commandline app
	}

}


