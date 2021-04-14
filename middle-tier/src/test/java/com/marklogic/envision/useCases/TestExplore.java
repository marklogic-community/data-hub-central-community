package com.marklogic.envision.useCases;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.controllers.AbstractMvcTest;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestExplore extends AbstractMvcTest {

	private static final String GET_RELATED_ENTITIES_TO_CONCEPT_URL = "/api/explore/related-entities-to-concept";

	@Autowired
	ModelService modelService;

	@BeforeEach
	public void setup() throws IOException {
		envisionConfig.setMultiTenant(true);
		super.setup();

		removeUser(ACCOUNT_NAME);
		removeUser(ACCOUNT_NAME2);
		clearStagingFinalAndJobDatabases();

		registerAccount();
		registerAccount(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
	}

	@Test
	void getRelatedConcepts() throws Exception {
		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("useCases/searchConcepts/model.json"));

		installDoc(hubClient.getFinalClient(), "useCases/searchConcepts/55000.json", "/envision/datahub/data/CoastalEmployees/55000.json", "MasterEmployees", "Employee", "MapCoastalEmployees", "sm-Employee-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "useCases/searchConcepts/e54aab0e-c280-4242-a5a9-ffdbfe47050b.json", "/envision/datahub/data/MountainTopEmployees/e54aab0e-c280-4242-a5a9-ffdbfe47050b.json", "MasterEmployees", "Employee", "MapEmployees", "sm-Employee-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "useCases/searchConcepts/aa58725f-a8a5-4133-a9f0-0ec0eeec88fc.json", "/envision/datahub/data/SonoranDesertEmployees/aa58725f-a8a5-4133-a9f0-0ec0eeec88fc.json", "DenormalizeEmployees", "Employee", "SonoranEmployees", "sm-Employee-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "useCases/searchConcepts/31cc48e1a9b992bda143be9514399b74.json", "/com.marklogic.smart-mastering/merged/31cc48e1a9b992bda143be9514399b74.json", "MasterEmployees", "Employee", "MapEmployees", "MapCoastalEmployees", "sm-Employee-merged", "sm-Employee-mastered", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);

		postJson(GET_RELATED_ENTITIES_TO_CONCEPT_URL, "{\"concept\":\"general ledger\",\"page\":1,\"pageLength\":10}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(GET_RELATED_ENTITIES_TO_CONCEPT_URL, "{\"concept\":\"general ledger\",\"page\":1,\"pageLength\":10}")
			.andDo(
				result -> {
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("useCases/searchConcepts/relatedConceptsOutput.json"), response);
				})
			.andExpect(status().isOk());
	}
}
