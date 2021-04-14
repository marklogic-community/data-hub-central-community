package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EntitiesControllerTests extends AbstractMvcTest {

	private static final String GET_ENTITIES_URL = "/api/entities";
	private static final String GET_ENTITY_URL = "/api/entities";

	@Autowired
	ModelService modelService;

	@BeforeEach
	public void setup() throws IOException {
		clearStagingFinalAndJobDatabases();
		envisionConfig.setMultiTenant(true);
		super.setup();
		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
	}

	@Test
	void getEntitiesTest() throws Exception {
		getJson(GET_ENTITIES_URL)
			.andExpect(status().isUnauthorized());

		login();

		getJson(GET_ENTITIES_URL)
			.andDo(
				result -> {
					assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
					ArrayNode entities = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(5, entities.size());
				})
			.andExpect(status().isOk());
	}

	@Test
	void getEntity() throws Exception {
		getJson(GET_ENTITY_URL + "/Planet")
			.andExpect(status().isUnauthorized());

		login();

		String filename = getNonAdminHubClient().getHubConfig().getHubProject().getHubEntitiesDir().resolve("Planet.entity.json").toAbsolutePath().toString();
		ObjectNode fileEntity = readJsonObject(getResourceFile("output/getEntityPlanet.json"));
		JsonNode expectedEntity = fileEntity.set("filename", new TextNode(filename));
		getJson(GET_ENTITY_URL + "/Planet")
			.andDo(
				result -> {
					assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
					JsonNode entity = readJsonObject(result.getResponse().getContentAsString());
					jsonAssertEquals(expectedEntity, entity);
				})
			.andExpect(status().isOk());
	}
}
