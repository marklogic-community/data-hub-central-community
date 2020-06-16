package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.envision.dataServices.EntitySearcher;
import com.marklogic.grove.boot.Application;
import com.marklogic.envision.model.ModelService;
import com.marklogic.grove.boot.search.SearchService;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

	private JsonNode allCollections;

	private final ObjectMapper om = new ObjectMapper();

	private CustomComparator resultCompare = new CustomComparator(JSONCompareMode.STRICT,
		new Customization("results[*].confidence", (o1, o2) -> true),
		new Customization("results[*].fitness", (o1, o2) -> true),
		new Customization("results[*].score", (o1, o2) -> true)
	);

	@BeforeEach
	void setUp() throws IOException {
		allCollections = om.readTree("[\"Department\", \"Employee\", \"Skill\"]");

		clearStagingFinalAndJobDatabases();

		installEnvisionModules();

		modelService.saveModel(getFinalClient(), getResourceStream("models/model.json"));
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
	}

	private String getFilterString(String filterString, int pageLength, DatabaseClient client) throws IOException {
		JsonNode filters = om.readTree(filterString);
		QueryManager mgr = client.newQueryManager();
		mgr.setPageLength(pageLength);
		StructuredQueryDefinition query = searchService.buildQueryWithCriteria(mgr.newStructuredQueryBuilder(), filters);
		return ((StructuredQueryDefinition) query).serialize();
	}

	// not collection
	//{"and":[{"type":"queryText","value":""},{"type":"selection","constraint":"Collections","constraintType":"collection","mode":"and","value":[{"not":"MasterEmployees"}]}]}
	@Test
	public void noresults() throws IOException, JSONException {
		DatabaseClient client = getFinalClient();
		int pageLength = 5;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities("fdljfkladjljad", 1, pageLength, "default", filterString);
		JSONAssert.assertEquals(getResource("output/noresults.json"), om.writeValueAsString(found), resultCompare);
	}

	@Test
	public void emptySearch_SortDefault_all() throws IOException, JSONException {
		DatabaseClient client = getFinalClient();
		int pageLength = 30;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(null, 1, pageLength, "default", filterString);
		JSONAssert.assertEquals(getResource("output/emptySearch_SortDefault_all.json"), om.writeValueAsString(found), resultCompare);
	}

	@Test
	public void emptySearch_SortDefault_noCollections() throws IOException, JSONException {
		DatabaseClient client = getFinalClient();
		int pageLength = 30;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(null, 1, pageLength, "default", filterString);
		JSONAssert.assertEquals(getResource("output/emptySearch_SortDefault_all.json"), om.writeValueAsString(found), resultCompare);
	}

	@Test
	public void emptySearch_SortDefault_onlyEmployee() throws IOException, JSONException {
		DatabaseClient client = getFinalClient();
		int pageLength = 5;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"},{\"type\":\"selection\",\"constraint\":\"Collections\",\"constraintType\":\"collection\",\"mode\":\"and\",\"value\":[\"Employee\"]}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(null, 1, pageLength, "default", filterString);
		JSONAssert.assertEquals(getResource("output/emptySearch_SortDefault_onlyEmployee.json"), om.writeValueAsString(found), resultCompare);
	}

	@Test
	public void emptySearch_SortDefault_onlyDepartment() throws IOException, JSONException {
		DatabaseClient client = getFinalClient();
		int pageLength = 5;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"},{\"type\":\"selection\",\"constraint\":\"Collections\",\"constraintType\":\"collection\",\"mode\":\"and\",\"value\":[\"Department\"]}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(null, 1, pageLength, "default", filterString);
		JSONAssert.assertEquals(getResource("output/emptySearch_SortDefault_onlyDepartment.json"), om.writeValueAsString(found), resultCompare);
	}

	@Test
	public void emptySearch_SortDefault_some() throws IOException, JSONException {
		DatabaseClient client = getFinalClient();
		int pageLength = 5;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(null, 1, pageLength, "default", filterString);
		JSONAssert.assertEquals(getResource("output/emptySearch_SortDefault_some.json"), om.writeValueAsString(found), resultCompare);
	}

	@Test
	public void pagination() throws IOException, JSONException {
		for (int page = 1; page <= 8; page++) {
			DatabaseClient client = getFinalClient();
			int pageLength = 1;
			String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"\"}]}", pageLength, client);
			JsonNode found = EntitySearcher.on(client).findEntities(null, page, pageLength, "default", filterString);
			JSONAssert.assertEquals(getResource("output/pagination" + page + ".json"), om.writeValueAsString(found), resultCompare);
		}
	}

	@Test
	public void search_hrskill3() throws IOException, JSONException {
		DatabaseClient client = getFinalClient();
		String qtext = "hrSkill3";
		int pageLength = 5;
		String filterString = getFilterString("{\"and\":[{\"type\":\"queryText\",\"value\":\"" + qtext + "\"}]}", pageLength, client);
		JsonNode found = EntitySearcher.on(client).findEntities(qtext, 1, pageLength, "default", filterString);
		JSONAssert.assertEquals(getResource("output/hrskill3.json"), om.writeValueAsString(found), resultCompare);
	}

	@Test
	public void getRelated() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).relatedEntities("/CoastalEmployees/55003.json", "belongsTo", 1, 10);
		JSONAssert.assertEquals(getResource("output/related-belongs-to.json"), om.writeValueAsString(found), resultCompare);

		JsonNode found2 = EntitySearcher.on(getFinalClient()).relatedEntities("/CoastalEmployees/55003.json", "has",1, 10);
		JSONAssert.assertEquals(getResource("output/related-has.json"), om.writeValueAsString(found2), resultCompare);
	}
}
