package com.marklogic.envision.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.SystemUtils;
import com.marklogic.grove.boot.AbstractController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController extends AbstractController {

	@RequestMapping(value = "/reset", method = RequestMethod.POST)
	JsonNode reset() {
		DatabaseClient client = getHubClient().getFinalClient();
		return SystemUtils.on(client).resetSystem();
	}
}
