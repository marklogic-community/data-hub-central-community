package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.envision.dataServices.EntityModeller;
import com.marklogic.envision.dataServices.EntitySearcher;
import com.marklogic.envision.model.ModelService;
import com.marklogic.envision.session.SessionPojo;
import com.marklogic.envision.session.SessionService;
import com.marklogic.grove.boot.Application;
import jdk.nashorn.internal.ir.ObjectNode;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Application.class)
public class ModelTests extends BaseTest {

	@Autowired
	ModelService modelService;

	@Autowired
	SessionService sessionService;

	@BeforeEach
	void setUp() throws IOException {
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		SessionPojo session = new SessionPojo();
		session.user = "user";
		session.currentModel = "TestModel.json";
		sessionService.saveSession(getFinalClient(), session);
	}

	@Test
	@WithMockUser
	public void toDatahub() throws IOException, JSONException {
		modelService.saveModel(getFinalClient(), getResourceStream("models/model.json"));
		DatabaseClient client = getFinalClient();
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode node = objectMapper.readTree(getResourceStream("models/model.json"));
		JsonNode result = EntityModeller.on(client).toDatahub(node);
		JSONAssert.assertEquals(getResource("output/esEntities.json"), om.writeValueAsString(result), true);
	}

	@Test
	public void fromDatahub() throws IOException, JSONException {
		CustomComparator resultCompare = new CustomComparator(JSONCompareMode.STRICT,
			new Customization("nodes.*.properties[*]._propId", (o1, o2) -> true)
		);

		installFinalDoc("esEntities/Department.entity.json", "/entities/Department.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Employee.entity.json", "/entities/Employee.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/MegaCorp.entity.json", "/entities/MegaCorp.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Organization.entity.json", "/entities/Organization.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Planet.entity.json", "/entities/Planet.entity.json", "http://marklogic.com/entity-services/models");

		DatabaseClient client = getFinalClient();
		JsonNode result = EntityModeller.on(client).fromDatahub();
		JSONAssert.assertEquals(getResource("output/esModel.json"), om.writeValueAsString(result), resultCompare);
	}
}
