package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ExportControllerTests extends AbstractMvcTest {

	private static final String GET_EXPORT_URL = "/api/export/runExports";

	@Autowired
	ModelService modelService;

	@BeforeEach
	void setup() throws IOException {
		logout();

		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		registerAccount();

		HubClient hc = getNonAdminHubClient();
		for (int i = 0; i < 100; i++) {
			installDoc(hc.getFinalClient(), "entities/employee1.json", "/col1/doc-" + i + ".json", "col1");
		}

		for (int i = 100; i < 300; i++) {
			installDoc(hc.getFinalClient(), "entities/employee1.json", "/col2/doc-" + i + ".json", "col2");
		}
	}

	@AfterEach
	void teardown() {
//		clearStagingFinalAndJobDatabases();
	}

	@Test
	void runExports() throws Exception {

		postJson(GET_EXPORT_URL, "[\"col1\", \"col2\"]")
			.andExpect(status().isUnauthorized());

		login();

		postJson(GET_EXPORT_URL, "[\"col1\", \"col2\"]")
			.andDo(
				result -> {
//					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
//					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
//					assertEquals(0, response.get("results").size());
				})
			.andExpect(status().isOk());

	}
}
