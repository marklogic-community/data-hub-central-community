package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TriplesControllerTests extends AbstractMvcTest {
	private static final String GET_TRIPLES_URL = "/api/triples/browse";
	private static final String GET_RELATED_TRIPLES_URL = "/api/triples/related";
	@Autowired
	ModelService modelService;

	@BeforeEach
	void setup() throws IOException {
		logout();

		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		registerAccount();
	}

	@Test
	void getTriples() throws Exception {
		postJson(GET_TRIPLES_URL, "{}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(GET_TRIPLES_URL, "{\"page\": 1, \"qtext\": null, \"subjectsPerPage\": 1, \"linksPerSubject\": 1, \"sort\": \"most-connected\",  \"dedup\": \"on\", \"database\": \"final\"}")
			.andDo(
				result -> {
					assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					jsonAssertEquals("{\"page\":1,\"subjectsPerPage\":1,\"linksPerSubject\":1,\"nodes\":{},\"edges\":{},\"total\":0}", response);
					assertEquals(1, response.get("page").asInt());
				})
			.andExpect(status().isOk());

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));

		postJson(GET_TRIPLES_URL, "{\"page\": 1, \"qtext\": null, \"subjectsPerPage\": 1, \"linksPerSubject\": 1, \"sort\": \"most-connected\",  \"dedup\": \"on\", \"database\": \"final\"}")
			.andDo(
				result -> {
					assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(185, response.get("total").asInt());
					assertEquals(1, response.get("page").asInt());
				})
			.andExpect(status().isOk());

		postJson(GET_TRIPLES_URL, "{\"page\": 1, \"qtext\": \"Department\", \"subjectsPerPage\": 1, \"linksPerSubject\": 1, \"sort\": \"most-connected\", \"dedup\": \"on\", \"database\": \"final\"}")
			.andDo(
				result -> {
					assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(67, response.get("total").asInt());
					assertEquals(1, response.get("page").asInt());
				})
			.andExpect(status().isOk());

		postJson(GET_TRIPLES_URL, "{\"page\": 2, \"qtext\": \"Department\", \"subjectsPerPage\": 1, \"linksPerSubject\": 1, \"sort\": \"most-connected\", \"dedup\": \"on\", \"database\": \"final\"}")
			.andDo(
				result -> {
					assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(67, response.get("total").asInt());
					assertEquals(2, response.get("page").asInt());
				})
			.andExpect(status().isOk());
	}

//	@Test
//	void getRelated() throws Exception {
//		postJson(GET_RELATED_TRIPLES_URL, "{}")
//			.andExpect(status().isUnauthorized());
//
//		login();
//
//		postJson(GET_RELATED_TRIPLES_URL, "{\"page\": 1, \"qtext\": null, \"subjectsPerPage\": 1, \"linksPerSubject\": 1, \"sort\": \"most-connected\", \"dedup\": \"on\", \"database\": \"final\"}")
//			.andDo(
//				result -> {
//                  assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
//					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
//					jsonAssertEquals("{\"page\":1,\"subjectsPerPage\":1,\"linksPerSubject\":1,\"nodes\":{},\"edges\":{},\"total\":0}", response);
//					assertEquals(1, response.get("page").asInt());
//				})
//			.andExpect(status().isOk());
//
//		HubClient hubClient = getNonAdminHubClient();
//		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
//
//		postJson(GET_TRIPLES_URL, "{\"page\": 1, \"qtext\": null, \"subjectsPerPage\": 1, \"linksPerSubject\": 1, \"sort\": \"most-connected\", \"dedup\": \"on\", \"database\": \"final\"}")
//			.andDo(
//				result -> {
//					assertTrue(result.getResponse().getHeader("Content-Type").startsWith("application/json"));
//					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
//					assertEquals(185, response.get("total").asInt());
//					assertEquals(1, response.get("page").asInt());
//				})
//			.andExpect(status().isOk());
//	}
}
