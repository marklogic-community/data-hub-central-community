package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.envision.export.ExportService;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ExportControllerTests extends AbstractMvcTest {

	private static final String GET_EXPORT_URL = "/api/export/runExports";
	private static final String GET_EXPORTS_URL = "/api/export";
	private static final String GET_FILE_URL = "/api/export/";

	@Autowired
	ModelService modelService;

	@Autowired
	ExportService exportService;

	@BeforeEach
	void setup() throws IOException {
		logout();

		removeUser(ACCOUNT_NAME);
		removeUser(ACCOUNT_NAME2);
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		registerAccount();
		registerAccount(ACCOUNT_NAME2, ACCOUNT_PASSWORD);

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
	void serviceExport() throws IOException {
		assertEquals(0, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));
		List<String> entityNames = new ArrayList<>();
		entityNames.add("col1");
		entityNames.add("col2");
		exportService.runExports(getNonAdminHubClient().getFinalClient(), getNonAdminHubClient().getUsername(), entityNames);
		assertEquals(1, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));
	}

	@Test
	void getExports() throws Exception {
		assertEquals(0, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));


		getJson(GET_EXPORTS_URL)
			.andExpect(status().isUnauthorized());

		login();

		getJson(GET_EXPORTS_URL)
			.andDo(
				result -> {
					ArrayNode response = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(0, response.size());
				})
			.andExpect(status().isOk());

		assertEquals(0, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));
		List<String> entityNames = new ArrayList<>();
		entityNames.add("col1");
		entityNames.add("col2");
		exportService.runExports(getNonAdminHubClient().getFinalClient(), getNonAdminHubClient().getUsername(), entityNames);
		exportService.runExports(getHubClient(ACCOUNT_NAME2, ACCOUNT_PASSWORD).getFinalClient(), getNonAdminHubClient().getUsername(), entityNames);

		assertEquals(2, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));

		getJson(GET_EXPORTS_URL)
			.andDo(
				result -> {
					ArrayNode response = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(1, response.size());
				})
			.andExpect(status().isOk());
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

	@Test
	void getFile() throws Exception {
		assertEquals(0, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));
		List<String> entityNames = new ArrayList<>();
		entityNames.add("col1");
		entityNames.add("col2");
		exportService.runExports(getNonAdminHubClient().getFinalClient(), getNonAdminHubClient().getUsername(), entityNames);
		exportService.runExports(getHubClient(ACCOUNT_NAME2, ACCOUNT_PASSWORD).getFinalClient(), getNonAdminHubClient().getUsername(), entityNames);
		assertEquals(2, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));

		String exportId = exportService.getExports(getNonAdminHubClient()).get(0).id;

		getJson(GET_FILE_URL + exportId)
			.andExpect(status().isUnauthorized());

		login();

		getJson(GET_FILE_URL + exportId)
			.andDo(
				result -> {
					assertEquals("application/zip", result.getResponse().getHeader("Content-Type"));
					ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
					ZipEntry zipEntry = zipInputStream.getNextEntry();
					assertEquals("col1.csv", zipEntry.getName());
					zipEntry = zipInputStream.getNextEntry();
					assertEquals("col2.csv", zipEntry.getName());
					zipEntry = zipInputStream.getNextEntry();
					assertNull(zipEntry);
				})
			.andExpect(status().isOk());
	}
}
