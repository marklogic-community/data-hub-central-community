package com.marklogic.envision.dataQuality;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.BaseTest;
import com.marklogic.envision.hub.HubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataQualityServiceTest extends BaseTest {

	private final CustomComparator resultCompare = new CustomComparator(JSONCompareMode.STRICT,
		new Customization("created", (o1, o2) -> true),
		new Customization("uri", (o1, o2) -> true)
	);

	@Autowired
	DataQualityService dataQualityService;

	@BeforeEach
	void setUp() throws IOException {
		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();

		installHubModules();
		installEnvisionModules();

		registerAccount();

		HubClient hubClient = getNonAdminHubClient();
		for (int i = 0; i < 10; i++) {
			installDoc(hubClient.getStagingClient(), "data/dataQuality/doc.json", "/doc-" + i + ".json", "testData");
		}
	}

	private void createFiles(DatabaseClient databaseClient) {
		for (int i = 0; i < 1001; i++) {
			installDoc(databaseClient, "data/dataQuality/doc.json", "/doc-" + i + ".json", "testData");
		}
	}

	@Test
	void testProfileData() throws Exception {
		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient stagingClient = hubClient.getStagingClient();

		clearStagingFinalAndJobDatabases();
		createFiles(stagingClient);
		assertEquals(0, getDocCount(stagingClient, "data-quality-report"));
		dataQualityService.profileData(getNonAdminHubClient(), "testData", "staging", 2);
		assertEquals(1, getDocCount(stagingClient, "data-quality-report"));
		jsonAssertEquals(getResource("data/dataQuality/profileReport2.json"), getCollectionDoc(stagingClient, "data-quality-report", null), resultCompare);

		clearStagingFinalAndJobDatabases();

		createFiles(stagingClient);
		assertEquals(0, getDocCount(stagingClient, "data-quality-report"));
		dataQualityService.profileData(getNonAdminHubClient(), "testData", "staging", 10);
		assertEquals(1, getDocCount(stagingClient, "data-quality-report"));
		jsonAssertEquals(getResource("data/dataQuality/profileReport10.json"), getCollectionDoc(stagingClient, "data-quality-report", null), resultCompare);

		clearStagingFinalAndJobDatabases();

		createFiles(stagingClient);
		assertEquals(0, getDocCount(stagingClient, "data-quality-report"));
		dataQualityService.profileData(getNonAdminHubClient(), "testData", "staging", 1000);
		assertEquals(1, getDocCount(stagingClient, "data-quality-report"));
		jsonAssertEquals(getResource("data/dataQuality/profileReport1000.json"), getCollectionDoc(stagingClient, "data-quality-report", null), resultCompare);

		clearStagingFinalAndJobDatabases();

		createFiles(stagingClient);
		assertEquals(0, getDocCount(stagingClient, "data-quality-report"));
		dataQualityService.profileData(getNonAdminHubClient(), "testData", "staging", 1001);
		assertEquals(1, getDocCount(stagingClient, "data-quality-report"));
		jsonAssertEquals(getResource("data/dataQuality/profileReport1001.json"), getCollectionDoc(stagingClient, "data-quality-report", null), resultCompare);

		clearStagingFinalAndJobDatabases();

		createFiles(stagingClient);
		assertEquals(0, getDocCount(stagingClient, "data-quality-report"));
		dataQualityService.profileData(getNonAdminHubClient(), "testData", "staging", 10000);
		assertEquals(1, getDocCount(stagingClient, "data-quality-report"));
		jsonAssertEquals(getResource("data/dataQuality/profileReport1001.json"), getCollectionDoc(stagingClient, "data-quality-report", null), resultCompare);


		clearStagingFinalAndJobDatabases();
		DatabaseClient finalClient = hubClient.getFinalClient();
		createFiles(finalClient);
		assertEquals(0, getDocCount(stagingClient, "data-quality-report"));
		dataQualityService.profileData(getNonAdminHubClient(), "testData", "final", 10000);
		assertEquals(1, getDocCount(stagingClient, "data-quality-report"));
		jsonAssertEquals(getResource("data/dataQuality/profileReport1001.json"), getCollectionDoc(stagingClient, "data-quality-report", null), resultCompare);
	}

	@Test
	void testGetReports() throws Exception {
		for (int i = 0; i < 37; i++) {
			installDoc(getNonAdminHubClient().getStagingClient(), "data/dataQuality/profileReport2.json", String.format("/data-quality/report/%s.json", UUID.randomUUID().toString()), "data-quality-report");
		}

		JsonNode reports = dataQualityService.getReports(getNonAdminHubClient(), 0, 10);
		assertEquals(37, reports.get("total").asInt());
		assertEquals(1, reports.get("page").asInt());
		assertEquals(10, reports.get("pageLength").asInt());
		assertEquals(10, reports.get("reports").size());
		jsonAssertEquals(getResource("data/dataQuality/reportListReport.json"), reports.get("reports").get(0), resultCompare);

		reports = dataQualityService.getReports(getNonAdminHubClient(), 1, 10);
		assertEquals(37, reports.get("total").asInt());
		assertEquals(1, reports.get("page").asInt());
		assertEquals(10, reports.get("pageLength").asInt());
		assertEquals(10, reports.get("reports").size());
		jsonAssertEquals(getResource("data/dataQuality/reportListReport.json"), reports.get("reports").get(0), resultCompare);

		reports = dataQualityService.getReports(getNonAdminHubClient(), 2, 10);
		assertEquals(37, reports.get("total").asInt());
		assertEquals(2, reports.get("page").asInt());
		assertEquals(10, reports.get("pageLength").asInt());
		assertEquals(10, reports.get("reports").size());
		jsonAssertEquals(getResource("data/dataQuality/reportListReport.json"), reports.get("reports").get(0), resultCompare);

		reports = dataQualityService.getReports(getNonAdminHubClient(), 3, 10);
		assertEquals(37, reports.get("total").asInt());
		assertEquals(3, reports.get("page").asInt());
		assertEquals(10, reports.get("pageLength").asInt());
		assertEquals(10, reports.get("reports").size());
		jsonAssertEquals(getResource("data/dataQuality/reportListReport.json"), reports.get("reports").get(0), resultCompare);

		reports = dataQualityService.getReports(getNonAdminHubClient(), 4, 10);
		assertEquals(37, reports.get("total").asInt());
		assertEquals(4, reports.get("page").asInt());
		assertEquals(10, reports.get("pageLength").asInt());
		assertEquals(7, reports.get("reports").size());
		jsonAssertEquals(getResource("data/dataQuality/reportListReport.json"), reports.get("reports").get(0), resultCompare);

		reports = dataQualityService.getReports(getNonAdminHubClient(), 5, 10);
		assertEquals(37, reports.get("total").asInt());
		assertEquals(5, reports.get("page").asInt());
		assertEquals(10, reports.get("pageLength").asInt());
		assertEquals(0, reports.get("reports").size());
	}

	@Test
	void getReport() throws Exception {
		DatabaseClient stagingClient = getNonAdminHubClient().getStagingClient();
		installDoc(stagingClient, "data/dataQuality/profileReport2.json", "/data-quality/report/test.json", "data-quality-report");

		JsonNode actual = dataQualityService.getReport(getNonAdminHubClient(), "/data-quality/report/test.json");
		jsonAssertEquals(getResource("data/dataQuality/profileReport2.json"), actual);
	}

	@Test
	void deleteReport() {
		DatabaseClient stagingClient = getNonAdminHubClient().getStagingClient();
		installDoc(stagingClient, "data/dataQuality/profileReport2.json", "/data-quality/report/test.json", "data-quality-report");

		assertEquals(1, getDocCount(stagingClient, "data-quality-report"));
		dataQualityService.deleteReport(getNonAdminHubClient(), "/data-quality/report/test.json");
		assertEquals(0, getDocCount(stagingClient, "data-quality-report"));
	}

	@Test
	void deleteAllReports() {
		DatabaseClient stagingClient = getNonAdminHubClient().getStagingClient();
		for (int i = 0; i < 37; i++) {
			installDoc(stagingClient, "data/dataQuality/profileReport2.json", String.format("/data-quality/report/%s.json", UUID.randomUUID().toString()), "data-quality-report");
		}

		assertEquals(37, getDocCount(stagingClient, "data-quality-report"));
		dataQualityService.deleteAllReports(getNonAdminHubClient());
		assertEquals(0, getDocCount(stagingClient, "data-quality-report"));
	}
}
