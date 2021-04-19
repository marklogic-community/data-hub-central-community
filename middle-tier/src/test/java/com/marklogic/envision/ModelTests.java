package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.EntityModeller;
import com.marklogic.envision.model.ModelService;
import com.marklogic.envision.session.SessionManager;
import com.marklogic.grove.boot.Application;
import com.marklogic.hub.HubConfig;
import org.junit.jupiter.api.AfterEach;
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

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Application.class)
public class ModelTests extends BaseTest {

	@Autowired
	ModelService modelService;

	@Autowired
	SessionManager sessionManager;

	private boolean modulesInstalled = false;

	@BeforeEach
	void setUp() throws Exception {
		envisionConfig.setMultiTenant(false);
		super.setup();
		deleteProtectedPaths(getAdminHubClient().getFinalClient());

		removeUser(ACCOUNT_NAME);


		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME);

		registerAccount();
		sessionManager.setHubClient(ACCOUNT_NAME, getAdminHubClient());
	}

	@AfterEach
	void teardown() {
		deleteProtectedPaths(getAdminHubClient().getFinalClient());
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahub() throws Exception {
		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir);
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/nestedModel.json"));
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		JsonNode result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/esEntities.json"), result);

		String actual = getDocumentString(getNonAdminHubClient().getFinalSchemasClient(), "/bob.smith@marklogic.com/employee-relationships-tde.json");
		System.out.println(actual);
		jsonAssertEquals(getResource("output/models/employeeRelationships.json"), actual);
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahubArrayOnly() throws Exception {
		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir);
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
		modelService.setModelsDir(modelsDir);
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
		jsonAssertEquals(getResource("output/esNestedModel.json"), result, resultCompare);
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahubNoRedaction() throws Exception {
		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));
		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir);
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/noRedaction.json"));
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		JsonNode result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/withRedaction.json"), result);
		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahubNoRedactionMultiTenant() throws Exception {
		envisionConfig.setMultiTenant(true);
		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME);
		installEnvisionModules();

		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));
		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule4bob.smith@marklogic.com"));
		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir);
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/noRedaction.json"));
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		JsonNode result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/withRedaction.json"), result);
		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));
		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule4bob.smith@marklogic.com"));
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahubWithRedactionMultiTenant() throws Exception {
		envisionConfig.setMultiTenant(true);
		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME);
		installEnvisionModules();

		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));
		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));

		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir);
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/withRedaction.json"));
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		JsonNode result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/withRedaction.json"), result);


		String actual = getDocumentString(getNonAdminHubClient().getFinalSchemasClient(), "/rules/redaction/bob.smith@marklogic.com/Employee-redactedProp.json");
		jsonAssertEquals(getResource("output/redactedPropSchema.json"), actual);

		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));
		assertEquals(1, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule4bob.smith@marklogic.com"));

		// now turn it off
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/noRedaction.json"));
		result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/withRedaction.json"), result);
		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule4bob.smith@marklogic.com"));
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahubWithRedaction() throws Exception {
		envisionConfig.setMultiTenant(true);
		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));

		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir);

		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/withRedaction.json"));
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		JsonNode result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/withRedaction.json"), result);


		String actual = getDocumentString(getNonAdminHubClient().getFinalSchemasClient(), "/rules/redaction/Employee-redactedProp.json");
		jsonAssertEquals(getResource("output/redactedPropSchema.json"), actual);

		assertEquals(1, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));

		// now turn it off
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/noRedaction.json"));
		result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/withRedaction.json"), result);
		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));

		// turn it back on
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/withRedaction.json"));
		result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/withRedaction.json"), result);

		// now remove the property
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/redactionRemove.json"));
		result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/redactionRemoved.json"), result);
		assertEquals(0, getDocCount(getNonAdminHubClient().getFinalSchemasClient(), "redactionRule"));

	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahubNoPii() throws Exception {
		Path protectedPaths = getNonAdminHubClient().getHubConfig().getUserSecurityDir().resolve("protected-paths");
		File[] files = protectedPaths.toFile().listFiles((dir, name) -> name.endsWith(HubConfig.PII_PROTECTED_PATHS_FILE));
		assertTrue(files == null || files.length == 0);
		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir);
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/noPii.json"));
		files = protectedPaths.toFile().listFiles((dir, name) -> name.endsWith(HubConfig.PII_PROTECTED_PATHS_FILE));
		assertTrue(files == null || files.length == 0);
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		JsonNode result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/noPii.json"), result);
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahubWithPii() throws Exception {
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir);

		Path protectedPaths = getNonAdminHubClient().getHubConfig().getUserSecurityDir().resolve("protected-paths");
		File[] files = protectedPaths.toFile().listFiles((dir, name) -> name.endsWith(HubConfig.PII_PROTECTED_PATHS_FILE));
		assertTrue(files == null || files.length == 0);

		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/withPii.json"));

		files = protectedPaths.toFile().listFiles((dir, name) -> name.endsWith(HubConfig.PII_PROTECTED_PATHS_FILE));
		assertEquals(1, files.length);

		JsonNode paths = getProtectedPaths(getAdminHubClient().getFinalClient());
		jsonAssertEquals("[\"/(es:envelope|envelope)/(es:instance|instance)/Employee/piiProp\"]", paths);

		JsonNode result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/withPii.json"), result);

		// turn off pii
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/noPii.json"));
		files = protectedPaths.toFile().listFiles((dir, name) -> name.endsWith(HubConfig.PII_PROTECTED_PATHS_FILE));
		assertTrue(files == null || files.length == 0);
		result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/noPii.json"), result);

		paths = getProtectedPaths(getAdminHubClient().getFinalClient());
		jsonAssertEquals("[]", paths);

		// turn it back on
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/withPii.json"));

		files = protectedPaths.toFile().listFiles((dir, name) -> name.endsWith(HubConfig.PII_PROTECTED_PATHS_FILE));
		assertEquals(1, files.length);

		paths = getProtectedPaths(getAdminHubClient().getFinalClient());
		jsonAssertEquals("[\"/(es:envelope|envelope)/(es:instance|instance)/Employee/piiProp\"]", paths);

		result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/withPii.json"), result);

		// delete pii property
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/piiRemoved.json"));
		files = protectedPaths.toFile().listFiles((dir, name) -> name.endsWith(HubConfig.PII_PROTECTED_PATHS_FILE));
		assertTrue(files == null || files.length == 0);
		result = EntityModeller.on(client).toDatahub();
		System.out.println(objectMapper.writeValueAsString(result));
		jsonAssertEquals(getResource("output/piiRemoved.json"), result);

		paths = getProtectedPaths(getAdminHubClient().getFinalClient());
		jsonAssertEquals("[]", paths);
	}
}
