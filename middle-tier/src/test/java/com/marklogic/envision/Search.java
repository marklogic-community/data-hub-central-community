package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.envision.dataServices.EntitySearcher;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import com.marklogic.envision.session.SessionManager;
import com.marklogic.grove.boot.Application;
import com.marklogic.grove.boot.search.SearchService;
import com.marklogic.hub.HubConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Application.class)
public class Search extends BaseTest {

	@Autowired
	ModelService modelService;

	@Autowired
	SearchService searchService;

	@Autowired
	SessionManager sessionManager;

	private final ObjectMapper om = new ObjectMapper();

	private final CustomComparator resultCompare = new CustomComparator(JSONCompareMode.STRICT,
		new Customization("results[*].confidence", (o1, o2) -> true),
		new Customization("results[*].fitness", (o1, o2) -> true),
		new Customization("results[*].score", (o1, o2) -> true)
	);

	private void localSetUp(boolean isMultiTenant) throws IOException {
		envisionConfig.setMultiTenant(isMultiTenant);
		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_STAGING_SCHEMAS_DB_NAME, HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME);

		installEnvisionModules();

		registerAccount();

		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient finalClient = hubClient.getFinalClient();

		modelService.setModelsDir(hubClient.getHubConfig().getHubProjectDir().resolve("conceptConnectorModels").toFile());
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
		installDoc(finalClient, "entities/employee-mastering-audit.xml", "/com.marklogic.smart-mastering/auditing/merge/87ab3989-912c-436c-809f-1b6c0b87f374.xml", "MasterEmployees", "sm-Employee-auditing", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "entities/employee1.json", "/CoastalEmployees/55002.json", "MasterEmployees", "MapCoastalEmployees", "sm-Employee-archived", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "entities/employee2.json", "/MountainTopEmployees/2d26f742-29b9-47f6-84d1-5f017ddf76d3.json", "MasterEmployees", "MapEmployees", "sm-Employee-archived", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "entities/employee-mastering-merged.json", "/com.marklogic.smart-mastering/merged/964e759b8ca1599896bf35c71c2fc0e8.json", "MasterEmployees", "MapCoastalEmployees", "MapEmployees", "sm-Employee-merged", "sm-Employee-mastered", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "entities/employee3.json", "/CoastalEmployees/55003.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "entities/employee4.json", "/MountainTopEmployees/employee4.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "entities/employee5.json", "/MountainTopEmployees/employee5.json", "MasterEmployees", "MapEmployees", "Employee", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);

		installDoc(finalClient, "entities/department2.json", "/departments/department2.json", "Department", "sm-Department-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "entities/department2_2.json", "/departments/department2_2.json", "Department", "sm-Department-archived", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "entities/department_mastered.json", "/com.marklogic.smart-mastering/merged/abcd759b8ca1599896bf35c71c2fc0e8.json", "MasterDepartment", "Department", "sm-Department-merged", "sm-Department-mastered", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "entities/department3.json", "/departments/department3.json", "Department", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
		installDoc(finalClient, "entities/department4.json", "/departments/department4.json", "Department", "http://marklogic.com/envision/user/" + ACCOUNT_NAME);
	}

	@AfterEach
	void teardown() {
		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_STAGING_SCHEMAS_DB_NAME, HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME);
	}

	private String getFilterString(String filterString, int pageLength, DatabaseClient client) throws IOException {
		JsonNode filters = om.readTree(filterString);
		QueryManager mgr = client.newQueryManager();
		mgr.setPageLength(pageLength);
		StructuredQueryDefinition query = searchService.buildQueryWithCriteria(mgr.newStructuredQueryBuilder(), filters);
		return query.serialize();
	}

	// not collection
	//{"and":[{"type":"queryText","value":""},{"type":"selection","constraint":"Collections","constraintType":"collection","mode":"and","value":[{"not":"MasterEmployees"}]}]}
	@WithMockUser(username = ACCOUNT_NAME)
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void noResults(boolean isMultiTenant) throws Exception {
		localSetUp(isMultiTenant);
		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient client = hubClient.getFinalClient();
		int pageLength = 5;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities("fdljfkladjljad", 1, pageLength, "default", filterString);
		jsonAssertEquals(getResource("output/noresults.json"), found, resultCompare);
	}

	@WithMockUser(username = ACCOUNT_NAME)
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void emptySearch_SortDefault_all(boolean isMultiTenant) throws Exception {
		localSetUp(isMultiTenant);
		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient client = hubClient.getFinalClient();
		int pageLength = 30;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(null, 1, pageLength, "default", filterString);
		System.out.println(objectMapper.writeValueAsString(found));
		jsonAssertEquals(getResource("output/emptySearch_SortDefault_all.json"), found, resultCompare);
	}

	@WithMockUser(username = ACCOUNT_NAME)
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void emptySearch_SortDefault_noCollections(boolean isMultiTenant) throws Exception {
		localSetUp(isMultiTenant);
		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient client = hubClient.getFinalClient();
		int pageLength = 30;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(null, 1, pageLength, "default", filterString);
		System.out.println(objectMapper.writeValueAsString(found));
		jsonAssertEquals(getResource("output/emptySearch_SortDefault_all.json"), found, resultCompare);
	}

	@WithMockUser(username = ACCOUNT_NAME)
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void emptySearch_SortDefault_onlyEmployee(boolean isMultiTenant) throws Exception {
		localSetUp(isMultiTenant);
		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient client = hubClient.getFinalClient();
		int pageLength = 5;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"},{\"type\":\"selection\",\"constraint\":\"Collections\",\"constraintType\":\"collection\",\"mode\":\"and\",\"value\":[\"Employee\"]}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(null, 1, pageLength, "default", filterString);
		System.out.println(objectMapper.writeValueAsString(found));
		jsonAssertEquals(getResource("output/emptySearch_SortDefault_onlyEmployee.json"), found, resultCompare);
	}

	@WithMockUser(username = ACCOUNT_NAME)
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void emptySearch_SortDefault_onlyDepartment(boolean isMultiTenant) throws Exception {
		localSetUp(isMultiTenant);
		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient client = hubClient.getFinalClient();
		int pageLength = 5;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"},{\"type\":\"selection\",\"constraint\":\"Collections\",\"constraintType\":\"collection\",\"mode\":\"and\",\"value\":[\"Department\"]}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(null, 1, pageLength, "default", filterString);
		System.out.println(objectMapper.writeValueAsString(found));
		jsonAssertEquals(getResource("output/emptySearch_SortDefault_onlyDepartment.json"), found, resultCompare);
	}

	@WithMockUser(username = ACCOUNT_NAME)
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void emptySearch_SortDefault_some(boolean isMultiTenant) throws Exception {
		localSetUp(isMultiTenant);
		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient client = hubClient.getFinalClient();
		int pageLength = 5;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(null, 1, pageLength, "default", filterString);
		System.out.println(objectMapper.writeValueAsString(found));
		jsonAssertEquals(getResource("output/emptySearch_SortDefault_some.json"), found, resultCompare);
	}

	@WithMockUser(username = ACCOUNT_NAME)
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void pagination(boolean isMultiTenant) throws Exception {
		localSetUp(isMultiTenant);
		for (int page = 1; page <= 8; page++) {
			HubClient hubClient = getNonAdminHubClient();
			DatabaseClient client = hubClient.getFinalClient();
			int pageLength = 1;
			String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"}]}", pageLength, client);
			JsonNode found = EntitySearcher.on(client).findEntities(null, page, pageLength, "default", filterString);
			System.out.println(objectMapper.writeValueAsString(found));
			jsonAssertEquals(getResource("output/pagination" + page + ".json"), found, resultCompare);
		}
	}

	@WithMockUser(username = ACCOUNT_NAME)
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void search_hrskill3(boolean isMultiTenant) throws Exception {
		localSetUp(isMultiTenant);
		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient client = hubClient.getFinalClient();
		String qtext = "hrSkill3";
		int pageLength = 5;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"" + qtext + "\"}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(qtext, 1, pageLength, "default", filterString);
		System.out.println(objectMapper.writeValueAsString(found));
		jsonAssertEquals(getResource("output/hrskill3.json"), found, resultCompare);
	}

	@WithMockUser(username = ACCOUNT_NAME)
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void getRelated(boolean isMultiTenant) throws Exception {
		localSetUp(isMultiTenant);
		HubClient hubClient = getNonAdminHubClient();
		DatabaseClient client = hubClient.getFinalClient();
		JsonNode found = EntitySearcher.on(client).relatedEntities("/CoastalEmployees/55003.json", "belongsTo", 1, 10);
		System.out.println(objectMapper.writeValueAsString(found));
		jsonAssertEquals(getResource("output/related-belongs-to.json"), found, resultCompare);

		JsonNode found2 = EntitySearcher.on(client).relatedEntities("/CoastalEmployees/55003.json", "has",1, 10);
		System.out.println(objectMapper.writeValueAsString(found2));
		jsonAssertEquals(getResource("output/related-has.json"), found2, resultCompare);
	}
}
