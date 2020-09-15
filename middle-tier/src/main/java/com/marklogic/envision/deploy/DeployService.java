package com.marklogic.envision.deploy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.client.ext.util.DefaultDocumentPermissionsParser;
import com.marklogic.client.ext.util.DocumentPermissionsParser;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.envision.commands.DeployEntitiesCommand;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.hub.deploy.commands.LoadUserArtifactsCommand;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.mapping.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeployService extends LoggingObject {

	private final DocumentPermissionsParser documentPermissionsParser = new DefaultDocumentPermissionsParser();

	final private LoadUserArtifactsCommand loadUserArtifactsCommand;

	private final ObjectMapper objectMapper = new ObjectMapper();

	protected JSONDocumentManager getFinalDocMgr(HubClient hubClient) {
		return hubClient.getFinalClient().newJSONDocumentManager();
	}

	protected JSONDocumentManager getStagingDocMgr(HubClient hubClient) {
		return hubClient.getStagingClient().newJSONDocumentManager();
	}

	@Autowired
	DeployService(LoadUserArtifactsCommand loadUserArtifactsCommand) {
		this.loadUserArtifactsCommand = loadUserArtifactsCommand;
	}

	public void deployEntities(HubClient hubClient) {
		try {
			new DeployEntitiesCommand(hubClient, hubClient.getUsername()).execute();
		}
		catch (Error error) {
			error.printStackTrace();
		}
	}

	public void loadMapping(HubClient hubClient, Mapping mapping) {
		String uri = getMappingUri(mapping.getName(), mapping.getVersion());
		DocumentMetadataHandle meta = buildMetadata("http://marklogic.com/data-hub/mappings", hubClient.getHubConfig().getModulePermissions());
		meta.getCollections().add(hubClient.getUsername() + "_mappings");
		StringHandle handle = new StringHandle(mapping.serialize());
		getStagingDocMgr(hubClient).write(uri, meta, handle);
		getFinalDocMgr(hubClient).write(uri, meta, handle);
	}

	public void deleteMapping(HubClient hubClient, String mappingName, int version) {
		String uri = getMappingUri(mappingName, version);
		getStagingDocMgr(hubClient).delete(uri);
		getFinalDocMgr(hubClient).delete(uri);
	}

	private String getMappingUri(String mappingName, int version) {
		return "/mappings/" + mappingName + "/" + mappingName + "-" + version + ".mapping.json";
	}

	public void loadFlow(HubClient hubClient, Flow flow) {
		String uri = "/flows/" + flow.getName() + ".flow.json";
		DocumentMetadataHandle meta = buildMetadata("http://marklogic.com/data-hub/flow", hubClient.getHubConfig().getModulePermissions());
		try {
			StringHandle handle = new StringHandle(objectMapper.writeValueAsString(flow));
			getStagingDocMgr(hubClient).write(uri, meta, handle);
			getFinalDocMgr(hubClient).write(uri, meta, handle);
		}
		catch(JsonProcessingException e) {
			throw new RuntimeException("Invalid Flow Json");
		}
	}

	public void deleteFlow(HubClient hubClient, String flowName) {
		String uri = "/flows/" + flowName + ".flow.json";
		getStagingDocMgr(hubClient).delete(uri);
		getFinalDocMgr(hubClient).delete(uri);
	}

	private DocumentMetadataHandle buildMetadata(String collection, String permissions) {
		DocumentMetadataHandle meta = new DocumentMetadataHandle();
		meta.getCollections().add(collection);
		documentPermissionsParser.parsePermissions(permissions, meta.getPermissions());
		return meta;
	}
}
