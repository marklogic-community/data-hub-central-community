package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.envision.dataServices.EntitySearcher;
import com.marklogic.grove.boot.Application;
import com.marklogic.envision.model.ModelService;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
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

	private JsonNode allCollections;

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

	@Test
	public void noresults() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).findEntities("fdljfkladjljad", 1, 5, "default", allCollections);
		JSONAssert.assertEquals(getResource("output/noresults.json"), om.writeValueAsString(found), true);
	}

	@Test
	public void emptySearch_SortDefault_all() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).findEntities(null, 1, 30, "default", allCollections);
		JSONAssert.assertEquals(getResource("output/emptySearch_SortDefault_all.json"), om.writeValueAsString(found), true);
	}

	@Test
	public void emptySearch_SortDefault_noCollections() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).findEntities(null, 1, 30, "default", om.readTree("[]"));
		JSONAssert.assertEquals(getResource("output/emptySearch_SortDefault_all.json"), om.writeValueAsString(found), true);
	}

	@Test
	public void emptySearch_SortDefault_onlyEmployee() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).findEntities(null, 1, 5, "default", om.readTree("[\"Employee\"]"));
		JSONAssert.assertEquals(getResource("output/emptySearch_SortDefault_onlyEmployee.json"), om.writeValueAsString(found), true);
	}

	@Test
	public void emptySearch_SortDefault_onlyDepartment() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).findEntities(null, 1, 5, "default", om.readTree("[\"Department\"]"));
		JSONAssert.assertEquals(getResource("output/emptySearch_SortDefault_onlyDepartment.json"), om.writeValueAsString(found), true);
	}

	@Test
	public void emptySearch_SortDefault_some() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).findEntities(null, 1, 5, "default", allCollections);
		JSONAssert.assertEquals(getResource("output/emptySearch_SortDefault_some.json"), om.writeValueAsString(found), true);
	}

	@Test
	public void emptySearch_SortMostConnected() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).findEntities(null, 1, 1, "mostConnected", allCollections);
		JSONAssert.assertEquals(getResource("output/emptySearch_SortMost.json"), om.writeValueAsString(found), true);
	}

	@Test
	public void emptySearch_SortLeastConnected() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).findEntities(null, 1, 1, "leastConnected", allCollections);
		JSONAssert.assertEquals(getResource("output/emptySearch_SortLeast.json"), om.writeValueAsString(found), true);
	}

	@Test
	public void pagination() throws IOException, JSONException {
		for (int page = 1; page <= 8; page++) {
			JsonNode found = EntitySearcher.on(getFinalClient()).findEntities(null, page, 1, "default", allCollections);
			JSONAssert.assertEquals(getResource("output/pagination" + page + ".json"), om.writeValueAsString(found), true);
		}
	}

	@Test
	public void search_hrskill3() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).findEntities("hrSkill3", 1, 5, "default", allCollections);
		JSONAssert.assertEquals(getResource("output/hrskill3.json"), om.writeValueAsString(found), true);
	}

	@Test
	public void getRelated() throws IOException, JSONException {
		JsonNode found = EntitySearcher.on(getFinalClient()).relatedEntities("/CoastalEmployees/55003.json", "belongsTo", 1, 10);
		JSONAssert.assertEquals(getResource("output/related-belongs-to.json"), om.writeValueAsString(found), true);

		JsonNode found2 = EntitySearcher.on(getFinalClient()).relatedEntities("/CoastalEmployees/55003.json", "has",1, 10);
		JSONAssert.assertEquals(getResource("output/related-has.json"), om.writeValueAsString(found2), true);
	}
}
