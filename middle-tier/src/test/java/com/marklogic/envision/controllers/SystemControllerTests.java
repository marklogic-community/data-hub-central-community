package com.marklogic.envision.controllers;

import com.marklogic.envision.hub.HubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SystemControllerTests extends AbstractMvcTest {
	private static final String RESET_URL = "/api/system/reset";
	private static final String DELETE_COLLECTIONS_URL = "/api/system/deleteCollection";

	@BeforeEach
	void setup() throws IOException {
		logout();

		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();
	}

	@Test
	void reset() throws Exception {
		registerAccount();

		postJson(RESET_URL, "{\"database\":\"staging\",\"collection\":\"user-data\"}")
			.andExpect(status().isUnauthorized());

		HubClient hubClient = getNonAdminHubClient();
		installDoc(hubClient.getStagingClient(), "data/stagingDoc.json", "/ingest/" + ACCOUNT_NAME + "/doc1.json", "user-data");
		installDoc(hubClient.getFinalClient(), "data/stagingDoc.json", "/data/" + ACCOUNT_NAME + "/doc1.json", "user-data");
		assertEquals(1, getDocCount(hubClient.getStagingClient(), "user-data"));
		assertEquals(1, getDocCount(hubClient.getFinalClient(), "user-data"));

		login();

		postJson(RESET_URL, "{\"database\":\"staging\",\"collection\":\"user-data\"}")
			.andExpect(status().isOk());

		assertEquals(0, getDocCount(hubClient.getStagingClient(), "user-data"));
		assertEquals(0, getDocCount(hubClient.getFinalClient(), "user-data"));
	}

	@Test
	void deleteCollection() throws Exception {
		registerAccount();

		postJson(DELETE_COLLECTIONS_URL, "{\"database\":\"staging\",\"collections\":[\"user-data\"]}")
			.andExpect(status().isUnauthorized());

		HubClient hubClient = getNonAdminHubClient();
		installDoc(hubClient.getStagingClient(), "data/stagingDoc.json", "/ingest/" + ACCOUNT_NAME + "/doc1.json", "user-data");
		assertEquals(1, getDocCount(hubClient.getStagingClient(), "user-data"));

		login();

		postJson(DELETE_COLLECTIONS_URL, "{\"database\":\"staging\",\"collections\":[\"user-data\"]}")
			.andExpect(status().isOk());

		assertEquals(0, getDocCount(hubClient.getStagingClient(), "user-data"));
	}

	@Test
	void deleteCollections() throws Exception {
		registerAccount();

		postJson(DELETE_COLLECTIONS_URL, "{\"database\":\"staging\",\"collections\":[\"user-data\"]}")
			.andExpect(status().isUnauthorized());

		HubClient hubClient = getNonAdminHubClient();
		installDoc(hubClient.getStagingClient(), "data/stagingDoc.json", "/ingest/" + ACCOUNT_NAME + "/doc1.json", "user-data");
		installDoc(hubClient.getStagingClient(), "data/stagingDoc.json", "/ingest/" + ACCOUNT_NAME + "/doc2.json", "user-data2");
		assertEquals(1, getDocCount(hubClient.getStagingClient(), "user-data"));
		assertEquals(1, getDocCount(hubClient.getStagingClient(), "user-data2"));

		login();

		postJson(DELETE_COLLECTIONS_URL, "{\"database\":\"staging\",\"collections\":[\"user-data\",\"user-data2\"]}")
			.andExpect(status().isOk());

		assertEquals(0, getDocCount(hubClient.getStagingClient(), "user-data"));
		assertEquals(0, getDocCount(hubClient.getStagingClient(), "user-data2"));
	}
}
