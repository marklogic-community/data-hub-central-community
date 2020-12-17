package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.envision.dataServices.Mastering;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import com.marklogic.grove.boot.Application;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Application.class)
public class MasteringTest extends BaseTest {

	@Autowired
	ModelService modelService;

	@BeforeEach
	void setUp() throws IOException {
		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();

		installEnvisionModules();

		registerAccount();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/model.json"));
		installDoc(hubClient.getFinalClient(), "entities/employee-mastering-audit.xml", "/com.marklogic.smart-mastering/auditing/merge/87ab3989-912c-436c-809f-1b6c0b87f374.xml", "MasterEmployees", "sm-Employee-auditing", "Employee");
		installDoc(hubClient.getFinalClient(), "entities/employee1.json", "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "sm-Employee-archived", "Employee");
		installDoc(hubClient.getFinalClient(), "entities/employee2.json", "/MountainTopEmployees/2d26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "sm-Employee-archived", "Employee");
		installDoc(hubClient.getFinalClient(), "entities/employee-mastering-merged.json", "/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json", "MasterEmployees", "MapCoastalEmployees", "MapEmployees", "sm-Employee-merged", "sm-Employee-mastered", "Employee");
		installDoc(hubClient.getFinalClient(), "entities/employee3.json", "/CoastalEmployees/55003.json", "MasterEmployees", "MapEmployees", "Employee");
		installDoc(hubClient.getFinalClient(), "entities/employee4.json", "/MountainTopEmployees/employee4.json", "MasterEmployees", "MapEmployees", "Employee");
		installDoc(hubClient.getFinalClient(), "entities/employee5.json", "/MountainTopEmployees/employee5.json", "MasterEmployees", "MapEmployees", "Employee");

		installDoc(hubClient.getFinalClient(), "entities/department2.json", "/departments/department2.json", "Department", "sm-Department-archived");
		installDoc(hubClient.getFinalClient(), "entities/department2_2.json", "/departments/department2_2.json", "Department", "sm-Department-archived");
		installDoc(hubClient.getFinalClient(), "entities/department_mastered.json", "/com.marklogic.smart-mastering/merged/abcd759b8ca1599896bf35c71c2fc0e8.json", "MasterDepartment", "Department", "sm-Department-merged", "sm-Department-mastered");
		installDoc(hubClient.getFinalClient(), "entities/department3.json", "/departments/department3.json", "Department");
		installDoc(hubClient.getFinalClient(), "entities/department4.json", "/departments/department4.json", "Department");

		installDoc(hubClient.getFinalClient(), "mastering/notification.xml", "/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml", "sm-Employee-notification", "Employee", "MasterEmployees");
		installDoc(hubClient.getFinalClient(), "mastering/notification-audit.xml", "/provenance/3122fb9289441afe347e3c38d30dfcf38ac45052df9543e61e48994c2e6a6dd6.xml", "http://marklogic.com/provenance-services/record");
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void unmerge() throws Exception {
		HubClient hubClient = getNonAdminHubClient();
		JsonNode found = Mastering.on(hubClient.getFinalClient()).unmerge("/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json");
		System.out.println(objectMapper.writeValueAsString(found));
		jsonAssertEquals(getResource("output/unmerge.json"), found);
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void block() throws Exception {
		ArrayNode uris = readJsonArray("[\"/CoastalEmployees/55003.json\", \"/MountainTopEmployees/employee4.json\"]");

		HubClient hubClient = getNonAdminHubClient();
		JsonNode preblocked = Mastering.on(hubClient.getFinalClient()).getBlocks(uris);
		jsonAssertEquals("{\"/CoastalEmployees/55003.json\":[],\"/MountainTopEmployees/employee4.json\":[]}", preblocked);

		Mastering.on(getFinalClient()).block(uris);

		JsonNode blocked = Mastering.on(hubClient.getFinalClient()).getBlocks(uris);
		jsonAssertEquals("{\"/CoastalEmployees/55003.json\":[\"/MountainTopEmployees/employee4.json\"],\"/MountainTopEmployees/employee4.json\":[\"/CoastalEmployees/55003.json\"]}", blocked);
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void updateStatus() throws Exception {
		HubClient hubClient = getNonAdminHubClient();
		JsonNode notification = Mastering.on(hubClient.getFinalClient()).getNotification("/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml");
		jsonAssertEquals(getResource("output/notification.json"), notification);
		Assert.assertEquals(notification.get("meta").get("status").asText(), "unread");

		Mastering.on(hubClient.getFinalClient()).updateNotifications(readJsonArray("[\"/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml\"]"), "read");

		JsonNode updated = Mastering.on(hubClient.getFinalClient()).getNotification("/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml");
		Assert.assertEquals(updated.get("meta").get("status").asText(), "read");

		Mastering.on(hubClient.getFinalClient()).updateNotifications(readJsonArray("[\"/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml\"]"), "unread");

		updated = Mastering.on(hubClient.getFinalClient()).getNotification("/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml");
		Assert.assertEquals(updated.get("meta").get("status").asText(), "unread");
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void getNotifications() throws Exception {
		JsonNode notifications = Mastering.on(getNonAdminHubClient().getFinalClient()).getNotifications("", 1, 10, "");
		jsonAssertEquals(getResource("output/notifications.json"), notifications);
	}
}
