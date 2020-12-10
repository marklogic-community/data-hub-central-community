package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.EntityModeller;
import com.marklogic.envision.model.ModelService;
import com.marklogic.envision.session.SessionManager;
import com.marklogic.grove.boot.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Application.class)
public class ModelTests extends BaseTest {

	@Autowired
	ModelService modelService;

	@Autowired
	SessionManager sessionManager;

	@BeforeEach
	void setUp() throws Exception {
		removeUser(ACCOUNT_NAME);

		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		registerAccount();
		sessionManager.setHubClient(ACCOUNT_NAME, getAdminHubClient());
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahub() throws Exception {
		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir.toFile());
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/nestedModel.json"));
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		JsonNode result = EntityModeller.on(client).toDatahub();
		jsonAssertEquals(getResource("output/esEntities.json"), result);
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahubArrayOnly() throws Exception {
		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir.toFile());
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/nestedModelArrayOnly.json"));
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		JsonNode result = EntityModeller.on(client).toDatahub();
		jsonAssertEquals(getResource("output/esEntitiesArrayOnly.json"), result);
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahubNoArray() throws Exception {
		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir.toFile());
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/nestedModelNoArray.json"));
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		JsonNode result = EntityModeller.on(client).toDatahub();
		jsonAssertEquals(getResource("output/esEntitiesNoArray.json"), result);
	}

	@Test
	public void fromDatahub() throws Exception {
		CustomComparator resultCompare = new CustomComparator(JSONCompareMode.STRICT,
			new Customization("nodes.*.properties[*]._propId", (o1, o2) -> true)
		);

		installFinalDoc("esEntities/Department.entity.json", "/entities/Department.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Employee.entity.json", "/entities/Employee.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Organization.entity.json", "/entities/Organization.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Address.entity.json", "/entities/Address.entity.json", "http://marklogic.com/entity-services/models");

		DatabaseClient client = getFinalClient();
		JsonNode result = EntityModeller.on(client).fromDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/esModel.json"), result, resultCompare);
	}

	@Test
	public void fromDatahubNested() throws Exception {
		CustomComparator resultCompare = new CustomComparator(JSONCompareMode.STRICT,
			new Customization("nodes.*.properties[*]._propId", (o1, o2) -> true),
			new Customization("nodes.*.properties[*].properties[*]._propId", (o1, o2) -> true)
		);

		installFinalDoc("esEntities/Address.entity.json", "/entities/Address.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/nestedEntities.json", "/entities/Employee.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Department.entity.json", "/entities/Department.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Organization.entity.json", "/entities/Organization.entity.json", "http://marklogic.com/entity-services/models");

		DatabaseClient client = getFinalClient();
		JsonNode result = EntityModeller.on(client).fromDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/esModel.json"), result, resultCompare);
	}
}
