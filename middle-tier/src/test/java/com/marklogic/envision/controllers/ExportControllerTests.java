package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.envision.export.ExportService;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import com.marklogic.hub.HubConfig;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ExportControllerTests extends AbstractMvcTest {

	private static final String GET_EXPORT_URL = "/api/export/runExports";
	private static final String GET_EXPORTS_URL = "/api/export/getExports";
	private static final String GET_FILE_URL = "/api/export/downloadExport";
	private static final String DELETE_FILE_URL = "/api/export/deleteExport";

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

		envisionConfig.setMultiTenant(true);
		installEnvisionModules();

		registerAccount();
		registerAccount(ACCOUNT_NAME2, ACCOUNT_PASSWORD);

		HubClient hc = getNonAdminHubClient();

		modelService.saveModel(hc, getResourceStream("entities/redactedName.json"));
		for (int i = 0; i < 100; i++) {
			installDoc(hc.getFinalClient(), "entities/exportMe.json", "/col1/doc-" + i + ".json", "Employee");
		}

		for (int i = 100; i < 300; i++) {
			installDoc(hc.getFinalClient(), "entities/exportMe2.xml", "/col2/doc-" + i + ".xml", "Department");
		}
	}

	@AfterEach
	void teardown() {
		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_STAGING_SCHEMAS_DB_NAME, HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME);
	}

	@Test
	void serviceExport() throws IOException {
		assertEquals(0, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));
		List<String> entityNames = new ArrayList<>();
		entityNames.add("Employee");
		entityNames.add("Department");
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
		entityNames.add("Employee");
		entityNames.add("Department");
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
		entityNames.add("Employee");
		entityNames.add("Department");
		exportService.runExports(getNonAdminHubClient().getFinalClient(), getNonAdminHubClient().getUsername(), entityNames);
		exportService.runExports(getHubClient(ACCOUNT_NAME2, ACCOUNT_PASSWORD).getFinalClient(), getNonAdminHubClient().getUsername(), entityNames);
		assertEquals(2, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));

		String exportId = exportService.getExports(getNonAdminHubClient()).get(0).id;

		getJson(GET_FILE_URL + "?exportId=" + exportId)
			.andExpect(status().isUnauthorized());

		login();

		getJson(GET_FILE_URL + "?exportId=" + exportId)
			.andDo(
				result -> {
					assertEquals("application/zip", result.getResponse().getHeader("Content-Type"));
					ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
					ZipEntry zipEntry = zipInputStream.getNextEntry();
					assertEquals("Employee.csv", zipEntry.getName());
					InputStream fileInputStream = readZipFileContents(zipInputStream);
					List<String> lines = IOUtils.readLines(fileInputStream);
					assertEquals("name,departmentId,employeeId,skills,skills2,json,empty", lines.get(0));
					assertEquals("### Redacted ###,\"2, has a comma\",\"55002 'hi'\",\"[\"\"general ledger\"\",\"\"not visible in search\"\"]\",\"[\"\"SPARQL\"\", \"\"XQuery\"\", \"\"Java\"\", \"\"RDF\"\"]\",\"{\"\"key\"\":\"\"value\"\"}\",", lines.get(1));

					zipEntry = zipInputStream.getNextEntry();
					assertEquals("Department.csv", zipEntry.getName());
					fileInputStream = readZipFileContents(zipInputStream);
					lines = IOUtils.readLines(fileInputStream);
					assertEquals("empty,departmentId,employeeIds,name", lines.get(0));
					assertEquals(",2,55002,\"Marketing, has a comma\"", lines.get(1));

					zipEntry = zipInputStream.getNextEntry();
					assertNull(zipEntry);
				})
			.andExpect(status().isOk());
	}

	private InputStream readZipFileContents(InputStream is) throws IOException {
		final byte[] contents = new byte[1024];
		int bytesRead;
		ByteArrayOutputStream streamBuilder = new ByteArrayOutputStream();
		while ((bytesRead = is.read(contents)) >= 0) {
			streamBuilder.write(contents, 0, bytesRead);
		}
		return new ByteArrayInputStream(streamBuilder.toByteArray());
	}

	@Test
	void deleteFile() throws Exception {
		assertEquals(0, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));
		List<String> entityNames = new ArrayList<>();
		entityNames.add("Employee");
		entityNames.add("Department");
		exportService.runExports(getNonAdminHubClient().getFinalClient(), getNonAdminHubClient().getUsername(), entityNames);
		exportService.runExports(getHubClient(ACCOUNT_NAME2, ACCOUNT_PASSWORD).getFinalClient(), getNonAdminHubClient().getUsername(), entityNames);
		assertEquals(2, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));

		String exportId = exportService.getExports(getNonAdminHubClient()).get(0).id;

		getJson(GET_FILE_URL + "?exportId=" + exportId)
			.andExpect(status().isUnauthorized());

		login();

		getJson(DELETE_FILE_URL + "?exportId=" + exportId)
			.andExpect(status().isOk());

		assertEquals(1, getDocCount(getAdminHubClient().getFinalClient(), ExportService.EXPORT_INFO_COLLECTION));
	}
}
