package com.marklogic.envision.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.SystemUtils;
import com.marklogic.grove.boot.AbstractController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
public class SystemController extends AbstractController {

	@RequestMapping(value = "/reset", method = RequestMethod.POST)
	JsonNode reset() {
		DatabaseClient client = getHubClient().getFinalClient();
		return SystemUtils.on(client).resetSystem();
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
