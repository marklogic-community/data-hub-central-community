package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.envision.dataServices.EntitySearcher;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ExploreControllerTests extends AbstractMvcTest {

	private static final String GET_ENTITIES_URL = "/api/explore/entities";
	private static final String GET_VALUES_URL = "/api/explore/values";
	private static final String GET_RELATED_ENTITIES_URL = "/api/explore/related-entities";
	private static final String GET_RELATED_ENTITIES_TO_CONCEPT_URL = "/api/explore/related-entities-to-concept";

	private final CustomComparator resultCompare = new CustomComparator(JSONCompareMode.STRICT,
		new Customization("results[*].confidence", (o1, o2) -> true),
		new Customization("results[*].fitness", (o1, o2) -> true),
		new Customization("results[*].score", (o1, o2) -> true)
	);

	@Autowired
	ModelService modelService;

	@BeforeEach
	void setup() throws IOException {
		logout();

		removeUser(ACCOUNT_NAME);
		removeUser(ACCOUNT_NAME2);
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		registerAccount();
		registerAccount(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
	}

	@Test
	void getEntitiesTest() throws Exception {
		postJson(GET_ENTITIES_URL, "{}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(0, response.get("results").size());
				})
			.andExpect(status().isOk());

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/explore/model.json"));
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

		// wait for indexing
		Thread.sleep(1000);

		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResults1.json"), response, resultCompare);
				})
			.andExpect(status().isOk());

		postJson(GET_ENTITIES_URL, "{ \"qtext\": \"Roberta Jones\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"Roberta Jones\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResults2.json"), response, resultCompare);
				})
			.andExpect(status().isOk());


		// redaction
		modelService.saveModel(hubClient, getResourceStream("models/explore/modelWithRedaction.json"));
		// wait for indexing
		Thread.sleep(1000);

		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResultsRedacted1.json"), response, resultCompare);
				})
			.andExpect(status().isOk());

		postJson(GET_ENTITIES_URL, "{ \"qtext\": \"Roberta Jones\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"Roberta Jones\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResultsRedacted2.json"), response, resultCompare);
				})
			.andExpect(status().isOk());

		// turn redaction back off
		modelService.saveModel(hubClient, getResourceStream("models/explore/model.json"));
		// wait for indexing
		Thread.sleep(1000);

		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResults1.json"), response, resultCompare);
				})
			.andExpect(status().isOk());

		postJson(GET_ENTITIES_URL, "{ \"qtext\": \"Roberta Jones\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"Roberta Jones\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResults2.json"), response, resultCompare);
				})
			.andExpect(status().isOk());
	}

	@Test
	void getEntitiesMultitenantRedactionTest() throws Exception {
		envisionConfig.setMultiTenant(true);
		installEnvisionModules();

		postJson(GET_ENTITIES_URL, "{}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(0, response.get("results").size());
				})
			.andExpect(status().isOk());

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/explore/model.json"));

		installDoc(hubClient.getFinalClient(), "entities/employee-mastering-audit.xml", "/com.marklogic.smart-mastering/auditing/merge/87ab3989-912c-436c-809f-1b6c0b87f374.xml", "MasterEmployees", "sm-Employee-auditing", "Employee");
		installDoc(hubClient.getFinalClient(), "entities/employee1.json", "/data/" + ACCOUNT_NAME + "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "sm-Employee-archived", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee2.json", "/data/" + ACCOUNT_NAME + "/MountainTopEmployees/2d26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "sm-Employee-archived", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee-mastering-merged.json", "/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json", "MasterEmployees", "MapCoastalEmployees", "MapEmployees", "sm-Employee-merged", "sm-Employee-mastered", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee3.json", "/data/" + ACCOUNT_NAME + "/CoastalEmployees/55003.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee4.json", "/data/" + ACCOUNT_NAME + "/MountainTopEmployees/employee4.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee5.json", "/data/" + ACCOUNT_NAME + "/MountainTopEmployees/employee5.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);

		installDoc(hubClient.getFinalClient(), "entities/department2.json", "/data/" + ACCOUNT_NAME + "/departments/department2.json", "Department", "sm-Department-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department2_2.json", "/data/" + ACCOUNT_NAME + "/departments/department2_2.json", "Department", "sm-Department-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department_mastered.json", "/com.marklogic.smart-mastering/merged/abcd759b8ca1599896bf35c71c2fc0e8.json", "MasterDepartment", "Department", "sm-Department-merged", "sm-Department-mastered", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department3.json", "/data/" + ACCOUNT_NAME + "/departments/department3.json", "Department", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department4.json", "/data/" + ACCOUNT_NAME + "/departments/department4.json", "Department", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);

		// wait for indexing
		Thread.sleep(1000);

		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResults1MT.json"), response, resultCompare);
				})
			.andExpect(status().isOk());

		postJson(GET_ENTITIES_URL, "{ \"qtext\": \"Roberta Jones\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"Roberta Jones\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResults2MT.json"), response, resultCompare);
				})
			.andExpect(status().isOk());


		// redaction
		modelService.saveModel(hubClient, getResourceStream("models/explore/modelWithRedaction.json"));
		// wait for indexing
		Thread.sleep(1000);

		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResultsRedacted1MT.json"), response, resultCompare);
				})
			.andExpect(status().isOk());

		postJson(GET_ENTITIES_URL, "{ \"qtext\": \"Roberta Jones\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"Roberta Jones\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResultsRedacted2MT.json"), response, resultCompare);
				})
			.andExpect(status().isOk());

		// turn redaction back off
		modelService.saveModel(hubClient, getResourceStream("models/explore/model.json"));
		// wait for indexing
		Thread.sleep(1000);

		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResults1MT.json"), response, resultCompare);
				})
			.andExpect(status().isOk());

		postJson(GET_ENTITIES_URL, "{ \"qtext\": \"Roberta Jones\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"Roberta Jones\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					System.out.println(objectMapper.writeValueAsString(response));
					jsonAssertEquals(getResource("output/explore/searchResults2MT.json"), response, resultCompare);
				})
			.andExpect(status().isOk());

		envisionConfig.setMultiTenant(false);
		installEnvisionModules();
	}

	@Test
	void getEntitiesMultiTenantTest() throws Exception {
		envisionConfig.setMultiTenant(true);
		installEnvisionModules();

		postJson(GET_ENTITIES_URL, "{}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(0, response.get("results").size());
				})
			.andExpect(status().isOk());

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/explore/model.json"));
		// wait for indexing
		Thread.sleep(1000);
		installDoc(hubClient.getFinalClient(), "entities/employee-mastering-audit.xml", "/com.marklogic.smart-mastering/auditing/merge/87ab3989-912c-436c-809f-1b6c0b87f374.xml", "MasterEmployees", "sm-Employee-auditing", "Employee");
		installDoc(hubClient.getFinalClient(), "entities/employee1.json", "/data/" + ACCOUNT_NAME + "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "sm-Employee-archived", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee2.json", "/data/" + ACCOUNT_NAME + "/MountainTopEmployees/2d26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "sm-Employee-archived", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee-mastering-merged.json", "/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json", "MasterEmployees", "MapCoastalEmployees", "MapEmployees", "sm-Employee-merged", "sm-Employee-mastered", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee3.json", "/data/" + ACCOUNT_NAME + "/CoastalEmployees/55003.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee4.json", "/data/" + ACCOUNT_NAME + "/MountainTopEmployees/employee4.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee5.json", "/data/" + ACCOUNT_NAME + "/MountainTopEmployees/employee5.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);

		installDoc(hubClient.getFinalClient(), "entities/department2.json", "/data/" + ACCOUNT_NAME + "/departments/department2.json", "Department", "sm-Department-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department2_2.json", "/data/" + ACCOUNT_NAME + "/departments/department2_2.json", "Department", "sm-Department-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department_mastered.json", "/com.marklogic.smart-mastering/merged/abcd759b8ca1599896bf35c71c2fc0e8.json", "MasterDepartment", "Department", "sm-Department-merged", "sm-Department-mastered", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department3.json", "/data/" + ACCOUNT_NAME + "/departments/department3.json", "Department", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department4.json", "/data/" + ACCOUNT_NAME + "/departments/department4.json", "Department", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);

		loginAsUser(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(0, response.get("results").size());
				})
			.andExpect(status().isOk());
		logout();

		hubClient = getHubClient(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
		modelService.saveModel(hubClient, getResourceStream("models/modelNoSkill.json"));
		// wait for indexing
		Thread.sleep(1000);

		installDoc(hubClient.getFinalClient(), "entities/employee-mastering-audit.xml", "/com.marklogic.smart-mastering/auditing/merge/88ab3989-912c-436c-809f-1b6c0b87f374.xml", "MasterEmployees", "sm-Employee-auditing", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);
		installDoc(hubClient.getFinalClient(), "entities/employee1.json", "/data/" + ACCOUNT_NAME2 + "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "sm-Employee-archived", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);
		installDoc(hubClient.getFinalClient(), "entities/employee2.json", "/data/" + ACCOUNT_NAME2 + "/MountainTopEmployees/2e26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "sm-Employee-archived", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);
		installDoc(hubClient.getFinalClient(), "entities/employee-mastering-merged.json", "/com.marklogic.smart-mastering/merged/974e759b8ca1599896bf35c71c2fc0e8.json", "MasterEmployees", "MapCoastalEmployees", "MapEmployees", "sm-Employee-merged", "sm-Employee-mastered", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);
		installDoc(hubClient.getFinalClient(), "entities/employee3.json", "/data/" + ACCOUNT_NAME2 + "/CoastalEmployees/55003.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);
		installDoc(hubClient.getFinalClient(), "entities/employee4.json", "/data/" + ACCOUNT_NAME2 + "/MountainTopEmployees/employee4.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);
		installDoc(hubClient.getFinalClient(), "entities/employee5.json", "/data/" + ACCOUNT_NAME2 + "/MountainTopEmployees/employee5.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);

		installDoc(hubClient.getFinalClient(), "entities/department2.json", "/data/" + ACCOUNT_NAME2 + "/departments/department2.json", "Department", "sm-Department-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);
		installDoc(hubClient.getFinalClient(), "entities/department2_2.json", "/data/" + ACCOUNT_NAME2 + "/departments/department2_2.json", "Department", "sm-Department-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);
		installDoc(hubClient.getFinalClient(), "entities/department_mastered.json", "/com.marklogic.smart-mastering/merged/abcde759b8ca1599896bf35c71c2fc0e8.json", "MasterDepartment", "Department", "sm-Department-merged", "sm-Department-mastered", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);
		installDoc(hubClient.getFinalClient(), "entities/department3.json", "/data/" + ACCOUNT_NAME2 + "/departments/department3.json", "Department", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);
		installDoc(hubClient.getFinalClient(), "entities/department4.json", "/data/" + ACCOUNT_NAME2 + "/departments/department4.json", "Department", "http://marklogic.com/envision/user/" + ACCOUNT_NAME2);

		loginAsUser(ACCOUNT_NAME, ACCOUNT_PASSWORD);
		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(7, response.get("results").size());
					assertEquals(7, response.get("total").asInt());
					assertEquals(13, response.get("nodes").size());
					assertEquals(24, response.get("edges").size());
				})
			.andExpect(status().isOk());

		logout();
		loginAsUser(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(7, response.get("results").size());
					assertEquals(7, response.get("total").asInt());
					assertEquals(7, response.get("nodes").size());
					assertEquals(8, response.get("edges").size());
				})
			.andExpect(status().isOk());

		logout();
		loginAsUser(ACCOUNT_NAME, ACCOUNT_PASSWORD);

		postJson(GET_ENTITIES_URL, "{ \"qtext\": \"Roberta Jones\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"Roberta Jones\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(1, response.get("results").size());
					assertEquals(1, response.get("total").asInt());
				})
			.andExpect(status().isOk());

		logout();
		loginAsUser(ACCOUNT_NAME2, ACCOUNT_PASSWORD);

		postJson(GET_ENTITIES_URL, "{ \"qtext\": \"Roberta Jones\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"Roberta Jones\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(1, response.get("results").size());
					assertEquals(1, response.get("total").asInt());
				})
			.andExpect(status().isOk());

		envisionConfig.setMultiTenant(false);
		installEnvisionModules();
	}

	@Test
	void getEntitiesPaginationTest() throws Exception {
		login();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
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

		for (int i = 1; i <= 7; i++) {
			int finalI = i;
			postJson(GET_ENTITIES_URL, "{ \"pageLength\": 1, \"page\": " + i + ", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
				.andDo(
					result -> {
						assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
						JsonNode response = readJsonObject(result.getResponse().getContentAsString());
						assertEquals(1, response.get("results").size());
						assertEquals(7, response.get("total").asInt());
						assertEquals(1, response.get("pageLength").asInt());
						assertEquals(finalI, response.get("page").asInt());
					})
				.andExpect(status().isOk());
		}

		postJson(GET_ENTITIES_URL, "{ \"pageLength\": 1, \"page\": 8, \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(0, response.get("results").size());
					assertEquals(7, response.get("total").asInt());
					assertEquals(1, response.get("pageLength").asInt());
					assertEquals(8, response.get("page").asInt());
				})
			.andExpect(status().isOk());
	}

	@Test
	void getEntitiesStagingTest() throws Exception {
		login();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
		installDoc(hubClient.getStagingClient(), "entities/employee-mastering-audit.xml", "/com.marklogic.smart-mastering/auditing/merge/87ab3989-912c-436c-809f-1b6c0b87f374.xml", "MasterEmployees", "sm-Employee-auditing", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee1.json", "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "sm-Employee-archived", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee2.json", "/MountainTopEmployees/2d26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "sm-Employee-archived", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee-mastering-merged.json", "/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json", "MasterEmployees", "MapCoastalEmployees", "MapEmployees", "sm-Employee-merged", "sm-Employee-mastered", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee3.json", "/CoastalEmployees/55003.json", "MasterEmployees", "MapEmployees", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee4.json", "/MountainTopEmployees/employee4.json", "MasterEmployees", "MapEmployees", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee5.json", "/MountainTopEmployees/employee5.json", "MasterEmployees", "MapEmployees", "Employee");

		installDoc(hubClient.getStagingClient(), "entities/department2.json", "/departments/department2.json", "Department", "sm-Department-archived");
		installDoc(hubClient.getStagingClient(), "entities/department2_2.json", "/departments/department2_2.json", "Department", "sm-Department-archived");
		installDoc(hubClient.getStagingClient(), "entities/department_mastered.json", "/com.marklogic.smart-mastering/merged/abcd759b8ca1599896bf35c71c2fc0e8.json", "MasterDepartment", "Department", "sm-Department-merged", "sm-Department-mastered");
		installDoc(hubClient.getStagingClient(), "entities/department3.json", "/departments/department3.json", "Department");
		installDoc(hubClient.getStagingClient(), "entities/department4.json", "/departments/department4.json", "Department");

		postJson(GET_ENTITIES_URL, "{ \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(0, response.get("results").size());
					assertEquals(0, response.get("total").asInt());
				})
			.andExpect(status().isOk());

		postJson(GET_ENTITIES_URL, "{ \"database\": \"staging\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(10, response.get("results").size());
					assertEquals(17, response.get("total").asInt());
				})
			.andExpect(status().isOk());
	}

	@Test
	void collectionQuery() throws Exception {
		login();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
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

		postJson(GET_ENTITIES_URL, "{\"filters\":{\"and\":[{\"type\":\"queryText\",\"value\":\"\"},{\"type\":\"selection\",\"constraint\":\"Collections\",\"constraintType\":\"collection\",\"mode\":\"and\",\"value\":[\"Department\"]}]}}")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(3, response.get("results").size());
					assertEquals(3, response.get("total").asInt());
				})
			.andExpect(status().isOk());
	}

	@Test
	void getValues() throws Exception {
		postJson(GET_VALUES_URL, "{}")
			.andExpect(status().isUnauthorized());

		login();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
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

		postJson(GET_VALUES_URL, "{\"facetName\": \"Collections\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(10, response.get("values-response").get("distinct-value").size());
				})
			.andExpect(status().isOk());


		postJson(GET_VALUES_URL, "{\"facetName\": \"Collections\", \"qtext\": \"Roberta Jones\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"Roberta Jones\" } ] } }")
				.andDo(
			result -> {
			assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
			JsonNode response = readJsonObject(result.getResponse().getContentAsString());
			assertEquals(3, response.get("values-response").get("distinct-value").size());
		})
		.andExpect(status().isOk());


		postJson(GET_VALUES_URL, "{\"facetName\": \"Collections\", \"filters\":{\"and\":[{\"type\":\"queryText\",\"value\":\"\"},{\"type\":\"selection\",\"constraint\":\"Collections\",\"constraintType\":\"collection\",\"mode\":\"and\",\"value\":[\"Department\"]}]}}")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(4, response.get("values-response").get("distinct-value").size());
				})
			.andExpect(status().isOk());
	}

	@Test
	void getValuesStaging() throws Exception {
		postJson(GET_VALUES_URL, "{}")
			.andExpect(status().isUnauthorized());

		login();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
		installDoc(hubClient.getStagingClient(), "entities/employee-mastering-audit.xml", "/com.marklogic.smart-mastering/auditing/merge/87ab3989-912c-436c-809f-1b6c0b87f374.xml", "MasterEmployees", "sm-Employee-auditing", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee1.json", "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "sm-Employee-archived", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee2.json", "/MountainTopEmployees/2d26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "sm-Employee-archived", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee-mastering-merged.json", "/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json", "MasterEmployees", "MapCoastalEmployees", "MapEmployees", "sm-Employee-merged", "sm-Employee-mastered", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee3.json", "/CoastalEmployees/55003.json", "MasterEmployees", "MapEmployees", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee4.json", "/MountainTopEmployees/employee4.json", "MasterEmployees", "MapEmployees", "Employee");
		installDoc(hubClient.getStagingClient(), "entities/employee5.json", "/MountainTopEmployees/employee5.json", "MasterEmployees", "MapEmployees", "Employee");

		installDoc(hubClient.getStagingClient(), "entities/department2.json", "/departments/department2.json", "Department", "sm-Department-archived");
		installDoc(hubClient.getStagingClient(), "entities/department2_2.json", "/departments/department2_2.json", "Department", "sm-Department-archived");
		installDoc(hubClient.getStagingClient(), "entities/department_mastered.json", "/com.marklogic.smart-mastering/merged/abcd759b8ca1599896bf35c71c2fc0e8.json", "MasterDepartment", "Department", "sm-Department-merged", "sm-Department-mastered");
		installDoc(hubClient.getStagingClient(), "entities/department3.json", "/departments/department3.json", "Department");
		installDoc(hubClient.getStagingClient(), "entities/department4.json", "/departments/department4.json", "Department");

		postJson(GET_VALUES_URL, "{\"facetName\": \"Collections\", \"database\": \"staging\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(10, response.get("values-response").get("distinct-value").size());
				})
			.andExpect(status().isOk());


		postJson(GET_VALUES_URL, "{\"facetName\": \"Collections\", \"database\": \"staging\", \"qtext\": \"Roberta Jones\", \"filters\": { \"and\": [ { \"type\": \"queryText\", \"value\": \"Roberta Jones\" } ] } }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(3, response.get("values-response").get("distinct-value").size());
				})
			.andExpect(status().isOk());
	}

	@Test
	void getRelatedEntities() throws Exception {

		postJson(GET_RELATED_ENTITIES_URL, "{}")
			.andExpect(status().isUnauthorized());

		login();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
		installDoc(hubClient.getFinalClient(), "entities/employee-mastering-audit.xml", "/com.marklogic.smart-mastering/auditing/merge/87ab3989-912c-436c-809f-1b6c0b87f374.xml", "MasterEmployees", "sm-Employee-auditing", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee1.json", "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "sm-Employee-archived", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee2.json", "/MountainTopEmployees/2d26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "sm-Employee-archived", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee-mastering-merged.json", "/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json", "MasterEmployees", "MapCoastalEmployees", "MapEmployees", "sm-Employee-merged", "sm-Employee-mastered", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee3.json", "/CoastalEmployees/55003.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee4.json", "/MountainTopEmployees/employee4.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/employee5.json", "/MountainTopEmployees/employee5.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);

		installDoc(hubClient.getFinalClient(), "entities/department2.json", "/departments/department2.json", "Department", "sm-Department-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department2_2.json", "/departments/department2_2.json", "Department", "sm-Department-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department_mastered.json", "/com.marklogic.smart-mastering/merged/abcd759b8ca1599896bf35c71c2fc0e8.json", "MasterDepartment", "Department", "sm-Department-merged", "sm-Department-mastered", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department3.json", "/departments/department3.json", "Department", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(hubClient.getFinalClient(), "entities/department4.json", "/departments/department4.json", "Department", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);

		postJson(GET_RELATED_ENTITIES_URL, "{\"uri\": \"/CoastalEmployees/55003.json\", \"label\": \"has\", \"page\": 1, \"pageLength\": 10 }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					jsonAssertEquals(getResource("output/related-has.json"), response);
				})
			.andExpect(status().isOk());

		postJson(GET_RELATED_ENTITIES_URL, "{\"uri\": \"/CoastalEmployees/55003.json\", \"label\": \"belongsTo\", \"page\": 1, \"pageLength\": 10 }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					jsonAssertEquals(getResource("output/related-belongs-to.json"), response);
				})
			.andExpect(status().isOk());
	}

	@Test
	void getRelatedEntitiesToConcept() throws Exception {
		postJson(GET_RELATED_ENTITIES_TO_CONCEPT_URL, "{}")
			.andExpect(status().isUnauthorized());

		login();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
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

		postJson(GET_RELATED_ENTITIES_TO_CONCEPT_URL, "{\"concept\": \"programming\", \"label\": \"has\", \"page\": 1, \"pageLength\": 10 }")
			.andDo(
				result -> {
					assertEquals("application/json;charset=UTF-8", result.getResponse().getHeader("Content-Type"));
					JsonNode response = readJsonObject(result.getResponse().getContentAsString());
					jsonAssertEquals(getResource("output/concept-related-programming.json"), response);
				})
			.andExpect(status().isOk());
	}
}
