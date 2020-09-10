package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FlowsControllerTests extends AbstractMvcTest {
	private static final String GET_FLOWS_URL = "/api/flows/";

	@BeforeEach
	void setup() throws IOException {
		logout();

		removeUser("bob.smith@marklogic.com");
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		registerAccount();
	}

	@Test
	void getFlows() throws Exception {
		getJson(GET_FLOWS_URL)
			.andExpect(status().isUnauthorized());

		loginAsUser(ACCOUNT_NAME, ACCOUNT_PASSWORD);
		getJson(GET_FLOWS_URL)
			.andDo(
				result -> {
					JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
				})
			.andExpect(status().isOk());
	}

	@Test
	void getFlow() throws Exception {
		getJson(GET_FLOWS_URL + "bogus")
			.andExpect(status().isUnauthorized());

		loginAsUser(ACCOUNT_NAME, ACCOUNT_PASSWORD);

		// doesn't exist
		getJson(GET_FLOWS_URL + "bogus")
			.andDo(
				result -> {
					JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
					JSONAssert.assertEquals(getResource("models/bogusModel.json"), objectMapper.writeValueAsString(node), true);
				})
			.andExpect(status().isOk());

		// already exists
		getJson(GET_FLOWS_URL + "bogus")
			.andDo(
				result -> {
					JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
					JSONAssert.assertEquals(getResource("models/bogusModel.json"), objectMapper.writeValueAsString(node), true);
				})
			.andExpect(status().isOk());
	}
}
