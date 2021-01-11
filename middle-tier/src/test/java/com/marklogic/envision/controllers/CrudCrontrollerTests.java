package com.marklogic.envision.controllers;

import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CrudCrontrollerTests extends AbstractMvcTest {

	private static final String GET_DOCUMENT_URL = "/api/crud";
	@Autowired
	ModelService modelService;

	@BeforeEach
	void setup() throws IOException {
		removeUser(ACCOUNT_NAME);
		removeUser(ACCOUNT_NAME2);
		clearStagingFinalAndJobDatabases();

		envisionConfig.setMultiTenant(true);
		registerAccount();
		registerAccount(ACCOUNT_NAME2, ACCOUNT_PASSWORD, "pii-reader");
		installEnvisionModules();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("entities/redactedName.json"));
		installDoc(hubClient.getFinalClient(), "entities/exportMe.json", "/doc.json", "col1", "Employee");

		HubClient user2Client = getHubClient(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
		installDoc(user2Client.getFinalClient(), "entities/exportMe.json", "/doc2.json", "col1", "Employee");

		installDoc(user2Client.getFinalClient(), "entities/redactionRolesDoc.json", "/redactionRules2Roles4" + ACCOUNT_NAME + ".json");
	}

	@Test
	void getRedactedDoc() throws Exception {
		getJson(GET_DOCUMENT_URL + "?uri=/doc.json")
			.andExpect(status().isUnauthorized());

		login();

		getJson(GET_DOCUMENT_URL + "?uri=/doc.json")
			.andDo(
				result -> {
					assertEquals("application/json", result.getResponse().getHeader("Content-Type"));
					String output = result.getResponse().getContentAsString();
					System.out.println(output);
					jsonAssertEquals(getResource("entities/exportMeRedacted.json"), readJsonObject(output));
				})
			.andExpect(status().isOk());

		logout();
		loginAsUser(ACCOUNT_NAME2, ACCOUNT_PASSWORD);

		getJson(GET_DOCUMENT_URL + "?uri=/doc2.json")
			.andDo(
				result -> {
					assertEquals("application/json", result.getResponse().getHeader("Content-Type"));
					String output = result.getResponse().getContentAsString();
					System.out.println(output);
					jsonAssertEquals(getResource("entities/exportMe.json"), readJsonObject(output));
				})
			.andExpect(status().isOk());
	}
}
