package com.marklogic.envision.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.SystemUtils;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.impl.DataHubImpl;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
public class SystemController extends AbstractController {

	@RequestMapping(value = "/reset", method = RequestMethod.POST)
	JsonNode reset() {
		boolean success = false;
		String error = "";
		ObjectNode objectNode = new ObjectMapper().createObjectNode();
		try {
			new DataHubImpl(getHubClient()).clearUserData();
			success = true;
		} catch(Exception ex) {
			error = ex.getMessage();
		}
		objectNode.put("success", success);
		objectNode.put("error", error);

		return (JsonNode) objectNode;
	}

	@RequestMapping(value = "/deleteCollection", method = RequestMethod.POST)
	void deleteCollection(@RequestBody DeleteCollectionPojo body) {
		DatabaseClient databaseClient;
		if (body.database.equals("final")) {
			databaseClient = getHubClient().getFinalClient();
		}
		else {
			databaseClient = getHubClient().getStagingClient();
		}
		SystemUtils.on(databaseClient).deleteDatasource(body.collections);
	}
}
