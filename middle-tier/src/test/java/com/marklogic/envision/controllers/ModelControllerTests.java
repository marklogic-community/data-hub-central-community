package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.envision.model.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ModelControllerTests extends AbstractMvcTest {
	private static final String CURRENT_MODEL_URL = "/api/models/current";

	@Autowired
	ModelService modelService;

	@BeforeEach
	void setup() throws IOException {
		logout();

		removeUser("bob.smith@marklogic.com");
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		registerAccount();

		modelService.setModelsDir(getHubClient().getHubConfig().getHubProjectDir().resolve("conceptConnectorModels").toFile());
	}

	@Test
	void getCurrentModelMissing() throws Exception {
		loginAsUser(ACCOUNT_NAME, ACCOUNT_PASSWORD);

		getJson(CURRENT_MODEL_URL)
			.andDo(
				result -> {
					JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());

				})
			.andExpect(status().isNotFound());
	}

	@Test
	void getCurrentModel() throws Exception {
		modelService.saveModel(getHubClient(ACCOUNT_NAME, ACCOUNT_PASSWORD), getResourceStream("models/model.json"));

		loginAsUser(ACCOUNT_NAME, ACCOUNT_PASSWORD);
		getJson(CURRENT_MODEL_URL)
			.andDo(
				result -> {
					JSONAssert.assertEquals(getResource("models/model.json"), result.getResponse().getContentAsString(), true);
				})
			.andExpect(status().isOk());
	}
}
