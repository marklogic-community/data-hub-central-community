package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.envision.dataServices.Mastering;
import com.marklogic.envision.model.ModelService;
import com.marklogic.grove.boot.Application;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
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
		clearStagingFinalAndJobDatabases();

		installEnvisionModules();

		modelService.saveModel(getHubClient(), getResourceStream("models/model.json"));
		installFinalDoc("entities/employee-mastering-audit.xml", "/com.marklogic.smart-mastering/auditing/merge/87ab3989-912c-436c-809f-1b6c0b87f374.xml", "MasterEmployees", "sm-Employee-auditing", "Employee");
		installFinalDoc("entities/employee1.json", "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "sm-Employee-archived", "Employee");
		installFinalDoc("entities/employee2.json", "/MountainTopEmployees/2d26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "sm-Employee-archived", "Employee");
		installFinalDoc("entities/employee-mastering-merged.json", "/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json", "MasterEmployees", "MapCoastalEmployees", "MapEmployees", "sm-Employee-merged", "sm-Employee-mastered", "Employee");
		installFinalDoc("entities/employee3.json", "/CoastalEmployees/55003.json", "MasterEmployees", "MapEmployees", "Employee");
		installFinalDoc("entities/employee4.json", "/MountainTopEmployees/employee4.json", "MasterEmployees", "MapEmployees", "Employee");
		installFinalDoc("entities/employee5.json", "/MountainTopEmployees/employee5.json", "MasterEmployees", "MapEmployees", "Employee");

		installFinalDoc("entities/department2.json", "/departments/department2.json", "Department", "sm-Department-archived");
		installFinalDoc("entities/department2_2.json", "/departments/department2_2.json", "Department", "sm-Department-archived");
		installFinalDoc("entities/department_mastered.json", "/com.marklogic.smart-mastering/merged/abcd759b8ca1599896bf35c71c2fc0e8.json", "MasterDepartment", "Department", "sm-Department-merged", "sm-Department-mastered");
		installFinalDoc("entities/department3.json", "/departments/department3.json", "Department");
		installFinalDoc("entities/department4.json", "/departments/department4.json", "Department");

		installFinalDoc("mastering/notification.xml", "/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml", "sm-Employee-notification", "Employee", "MasterEmployees");
		installFinalDoc("mastering/notification-audit.xml", "/provenance/3122fb9289441afe347e3c38d30dfcf38ac45052df9543e61e48994c2e6a6dd6.xml", "http://marklogic.com/provenance-services/record");
	}

	@Test
	@WithMockUser
	public void unmerge() throws IOException, JSONException {
		JsonNode found = Mastering.on(getFinalClient()).unmerge("/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json");
		JSONAssert.assertEquals(getResource("output/unmerge.json"), this.objectMapper.writeValueAsString(found), true);
	}

	@Test
	@WithMockUser
	public void block() throws IOException, JSONException {
		ArrayNode uris = (ArrayNode) objectMapper.readTree("[\"/CoastalEmployees/55003.json\", \"/MountainTopEmployees/employee4.json\"]");

		JsonNode preblocked = Mastering.on(getFinalClient()).getBlocks(uris);
		JSONAssert.assertEquals("{\"/CoastalEmployees/55003.json\":[],\"/MountainTopEmployees/employee4.json\":[]}", objectMapper.writeValueAsString(preblocked), true);

		Mastering.on(getFinalClient()).block(uris);

		JsonNode blocked = Mastering.on(getFinalClient()).getBlocks(uris);
		JSONAssert.assertEquals("{\"/CoastalEmployees/55003.json\":[\"/MountainTopEmployees/employee4.json\"],\"/MountainTopEmployees/employee4.json\":[\"/CoastalEmployees/55003.json\"]}", objectMapper.writeValueAsString(blocked), true);
	}

	@Test
	@WithMockUser
	public void updateStatus() throws IOException, JSONException {
		JsonNode notification = Mastering.on(getFinalClient()).getNotification("/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml");
		JSONAssert.assertEquals(getResource("output/notification.json"), this.objectMapper.writeValueAsString(notification), true);
		Assert.assertEquals(notification.get("meta").get("status").asText(), "unread");

		Mastering.on(getFinalClient()).updateNotifications(this.objectMapper.readTree("[\"/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml\"]"), "read");

		JsonNode updated = Mastering.on(getFinalClient()).getNotification("/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml");
		Assert.assertEquals(updated.get("meta").get("status").asText(), "read");

		Mastering.on(getFinalClient()).updateNotifications(this.objectMapper.readTree("[\"/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml\"]"), "unread");

		updated = Mastering.on(getFinalClient()).getNotification("/com.marklogic.smart-mastering/matcher/notifications/3b6cd608da7d7c596bd37e211207d2c8.xml");
		Assert.assertEquals(updated.get("meta").get("status").asText(), "unread");
	}

	@Test
	@WithMockUser
	public void getNotifications() throws IOException, JSONException {
		JsonNode notifications = Mastering.on(getFinalClient()).getNotifications("", 1, 10, "");
		JSONAssert.assertEquals(getResource("output/notifications.json"), this.objectMapper.writeValueAsString(notifications), true);
	}
}
