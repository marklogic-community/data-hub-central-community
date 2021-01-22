package com.marklogic.envision.dataQuality;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.controllers.AbstractMvcTest;
import com.marklogic.envision.hub.HubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DataQualityControllerTests extends AbstractMvcTest {
	private static final String PROFILE_DATA_URL = "/api/data-profiler/profile";
	private static final String GET_REPORTS_URL = "/api/data-profiler/reports";
	private static final String GET_REPORT_URL = "/api/data-profiler/report";
	private static final String DELETE_REPORT_URL = "/api/data-profiler/delete-report";
	private static final String DELETE_ALL_REPORTS_URL = "/api/data-profiler/delete-all-reports";
	private DatabaseClient stagingClient;

	private final CustomComparator resultCompare = new CustomComparator(JSONCompareMode.STRICT,
		new Customization("created", (o1, o2) -> true)
	);

	@BeforeEach
	void setup() throws IOException {
		removeUser(ACCOUNT_NAME);
		removeUser(ACCOUNT_NAME2);
		clearStagingFinalAndJobDatabases();

		installHubModules();
		installEnvisionModules();

		envisionConfig.setMultiTenant(true);
		registerAccount();

		HubClient hubClient = getNonAdminHubClient();
		stagingClient = hubClient.getStagingClient();
		for (int i = 0; i < 1001; i++) {
			installDoc(stagingClient, "data/dataQuality/doc.json", "/doc-" + i + ".json", "testData");
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {2, 10, 1000, 1001, 10000})
	void profileData(int limit) throws Exception {
		assertEquals(0, getDocCount(stagingClient, "data-quality-report"));

		postJson(PROFILE_DATA_URL, "{\"collection\":\"testData\",\"database\":\"staging\",\"sampleSize\":" + limit + "}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(PROFILE_DATA_URL, "{\"collection\":\"testData\",\"database\":\"staging\",\"sampleSize\":" + limit + "}")
			.andExpect(status().isOk());

		int retries = 0;
		while (getDocCount(stagingClient, "data-quality-report") < 1 && retries < 10) {
			retries++;
			Thread.sleep(1000);
		}
		assertEquals(1, getDocCount(stagingClient, "data-quality-report"));
		int reportLimit = Math.min(1001, limit);
		jsonAssertEquals(getResource("data/dataQuality/profileReport" + reportLimit + ".json"), getCollectionDoc(stagingClient, "data-quality-report", null), resultCompare);
	}

	@Test
	void getReports() throws Exception {
		for (int i = 0; i < 37; i++) {
			installDoc(getNonAdminHubClient().getStagingClient(), "data/dataQuality/profileReport2.json", String.format("/data-quality/report/%s.json", UUID.randomUUID().toString()), "data-quality-report");
		}

		postJson(GET_REPORTS_URL, "{\"page\":\"1\",\"pageLength\":10}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(GET_REPORTS_URL, "{\"page\":\"0\",\"pageLength\":10}")
			.andDo(
				result -> {
					ObjectNode reports = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(37, reports.get("total").asInt());
					assertEquals(1, reports.get("page").asInt());
					assertEquals(10, reports.get("pageLength").asInt());
					assertEquals(10, reports.get("reports").size());
				})
			.andExpect(status().isOk());

		postJson(GET_REPORTS_URL, "{\"page\":\"1\",\"pageLength\":10}")
			.andDo(
				result -> {
					ObjectNode reports = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(37, reports.get("total").asInt());
					assertEquals(1, reports.get("page").asInt());
					assertEquals(10, reports.get("pageLength").asInt());
					assertEquals(10, reports.get("reports").size());
				})
			.andExpect(status().isOk());

		postJson(GET_REPORTS_URL, "{\"page\":\"2\",\"pageLength\":10}")
			.andDo(
				result -> {
					ObjectNode reports = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(37, reports.get("total").asInt());
					assertEquals(2, reports.get("page").asInt());
					assertEquals(10, reports.get("pageLength").asInt());
					assertEquals(10, reports.get("reports").size());
				})
			.andExpect(status().isOk());

		postJson(GET_REPORTS_URL, "{\"page\":\"3\",\"pageLength\":10}")
			.andDo(
				result -> {
					ObjectNode reports = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(37, reports.get("total").asInt());
					assertEquals(3, reports.get("page").asInt());
					assertEquals(10, reports.get("pageLength").asInt());
					assertEquals(10, reports.get("reports").size());
				})
			.andExpect(status().isOk());

		postJson(GET_REPORTS_URL, "{\"page\":\"4\",\"pageLength\":10}")
			.andDo(
				result -> {
					ObjectNode reports = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(37, reports.get("total").asInt());
					assertEquals(4, reports.get("page").asInt());
					assertEquals(10, reports.get("pageLength").asInt());
					assertEquals(7, reports.get("reports").size());
				})
			.andExpect(status().isOk());

		postJson(GET_REPORTS_URL, "{\"page\":\"5\",\"pageLength\":10}")
			.andDo(
				result -> {
					ObjectNode reports = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(37, reports.get("total").asInt());
					assertEquals(5, reports.get("page").asInt());
					assertEquals(10, reports.get("pageLength").asInt());
					assertEquals(0, reports.get("reports").size());
				})
			.andExpect(status().isOk());
	}

	@Test
	void getReport() throws Exception {
		installDoc(getNonAdminHubClient().getStagingClient(), "data/dataQuality/profileReport2.json", "/data-quality/report/test.json", "data-quality-report");
		getJson(GET_REPORT_URL + "?uri=/data-quality/report/test.json")
			.andExpect(status().isUnauthorized());

		login();

		getJson(GET_REPORT_URL + "?uri=/data-quality/report/test.json")
			.andDo(
				result -> {
					ObjectNode report = readJsonObject(result.getResponse().getContentAsString());
					jsonAssertEquals(getResource("data/dataQuality/profileReport2.json"), report);
				})
			.andExpect(status().isOk());
	}

	@Test
	void deleteReport() throws Exception {

		installDoc(getNonAdminHubClient().getStagingClient(), "data/dataQuality/profileReport2.json", "/data-quality/report/test.json", "data-quality-report");

		assertEquals(1, getDocCount(getNonAdminHubClient().getStagingClient(), "data-quality-report"));
		getJson(DELETE_REPORT_URL + "?uri=/data-quality/report/test.json")
			.andExpect(status().isUnauthorized());

		login();

		getJson(DELETE_REPORT_URL + "?uri=/data-quality/report/test.json")
			.andExpect(status().isOk());

		assertEquals(0, getDocCount(getNonAdminHubClient().getStagingClient(), "data-quality-report"));
	}

	@Test
	void deleteAllReports() throws Exception {

		for (int i = 0; i < 37; i++) {
			installDoc(getNonAdminHubClient().getStagingClient(), "data/dataQuality/profileReport2.json", String.format("/data-quality/report/%s.json", UUID.randomUUID().toString()), "data-quality-report");
		}

		assertEquals(37, getDocCount(getNonAdminHubClient().getStagingClient(), "data-quality-report"));
		getJson(DELETE_ALL_REPORTS_URL)
			.andExpect(status().isUnauthorized());

		login();

		getJson(DELETE_ALL_REPORTS_URL)
			.andExpect(status().isOk());

		assertEquals(0, getDocCount(getNonAdminHubClient().getStagingClient(), "data-quality-report"));
	}
}
