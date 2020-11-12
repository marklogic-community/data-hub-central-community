package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.envision.deploy.DeployService;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import com.marklogic.hub.EntityManager;
import com.marklogic.hub.deploy.commands.DeployHubDatabaseCommand;
import com.marklogic.hub.entity.HubEntity;
import com.marklogic.hub.impl.EntityManagerImpl;
import com.marklogic.hub.impl.HubConfigImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ModelControllerTests extends AbstractMvcTest {
	private static final String ALL_MODELS_URL = "/api/models/";
	private static final String CURRENT_MODEL_URL = "/api/models/current";
	private static final String DELETE_MODEL_URL = "/api/models/delete";
	private static final String SAVE_MODEL_URL = "/api/models/";
	private static final String IMPORT_MODEL_URL = "/api/models/import";
	private static final String RENAME_MODEL_URL = "/api/models/rename";
	private static final String GET_ACTIVE_INDEXES_URL = "/api/models/activeIndexes";

	@Autowired
	ModelService modelService;

	@Autowired
	DeployService deployService;

	EntityManager getEntityManager(String username, String password) {
		return new EntityManagerImpl(getHubClient(username, password).getHubConfig());
	}

	EntityManager getEntityManager() {
		return getEntityManager(ACCOUNT_NAME, ACCOUNT_PASSWORD);
	}

	protected void removeModelFiles(boolean isMultiTenant, String username) {
		File modelDir = modelService.getModelsDir(isMultiTenant, username);
		for (File file: Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json")))) {
			file.delete();
		}
	}

	protected void removeEntityFiles() throws IOException {
		EntityManager entityManager = getEntityManager();
		for (HubEntity entity : entityManager.getEntities()) {
			entityManager.deleteEntity(entity.getInfo().getTitle());
		}
	}

	@BeforeEach
	void setup() throws IOException {
		logout();

		// remove models
		removeModelFiles(false, null);
		removeModelFiles(true, ACCOUNT_NAME);
		removeModelFiles(true, ACCOUNT_NAME2);
		envisionConfig.setMultiTenant(true);
		removeEntityFiles();

		envisionConfig.setMultiTenant(false);
		removeEntityFiles();


		removeUser(ACCOUNT_NAME);
		removeUser(ACCOUNT_NAME2);
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();
	}

	@Test
	void getCurrentModelMissing() throws Exception {
		registerAccount();
		login();

		getJson(CURRENT_MODEL_URL)
			.andExpect(status().isNotFound());
	}

	@Test
	void getCurrentModel() throws Exception {
		registerAccount();
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/model.json"));

		getJson(CURRENT_MODEL_URL)
			.andExpect(status().isUnauthorized());

		login();
		getJson(CURRENT_MODEL_URL)
			.andDo(
				result -> jsonAssertEquals(getResource("models/model.json"), result.getResponse().getContentAsString()))
			.andExpect(status().isOk());
	}

	@Test
	void getAllModelsMultiTenant() throws Exception {
		envisionConfig.setMultiTenant(true);
		getJson(ALL_MODELS_URL)
			.andExpect(status().isUnauthorized());

		registerAccount();
		registerAccount(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
		login();
		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(1, models.size());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);
				})
			.andExpect(status().isOk());

		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/model.json"));
		modelService.saveModel(getHubClient(ACCOUNT_NAME2, ACCOUNT_PASSWORD), getResourceStream("models/model.json"));

		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(2, models.size());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);
					JSONAssert.assertEquals(getResource("models/model.json"), objectMapper.writeValueAsString(models.get(1)), true);
				})
			.andExpect(status().isOk());

		getEntityManager().saveEntity(HubEntity.fromJson("Orphan.entity.json", readJsonObject(getResourceStream("esEntities/Orphan.entity.json"))), false);
		getEntityManager(ACCOUNT_NAME2, ACCOUNT_PASSWORD).saveEntity(HubEntity.fromJson("Orphan.entity.json", readJsonObject(getResourceStream("esEntities/Orphan.entity.json"))), false);
		deployService.deployEntities(getNonAdminHubClient());

		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(3, models.size());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);

					// get around sorting issues by getting the proper indexes for comparison
					// prevents failing tests in CI
					List<String> names = StreamSupport.stream(models.spliterator(), false).map(jsonNode -> jsonNode.get("name").asText()).collect(Collectors.toList());
					int idx = names.indexOf("My Hub Model");
					assertEquals("My Hub Model", models.get(idx).get("name").asText());
					idx = names.indexOf("Test Model");
					JSONAssert.assertEquals(getResource("models/model.json"), objectMapper.writeValueAsString(models.get(idx)), true);
				})
			.andExpect(status().isOk());
	}

	@Test
	void getAllModelsSingleTenant() throws Exception {
		envisionConfig.setMultiTenant(false);
		getJson(ALL_MODELS_URL)
			.andExpect(status().isUnauthorized());

		registerAccount();
		login();
		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);
				})
			.andExpect(status().isOk());

		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/model.json"));

		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(2, models.size());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);
					JSONAssert.assertEquals(getResource("models/model.json"), objectMapper.writeValueAsString(models.get(1)), true);
				})
			.andExpect(status().isOk());

		getEntityManager().saveEntity(HubEntity.fromJson("Orphan.entity.json", readJsonObject(getResourceStream("esEntities/Orphan.entity.json"))), false);
		deployService.deployEntities(getNonAdminHubClient());

		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(3, models.size());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);

					// get around sorting issues by getting the proper indexes for comparison
					// prevents failing tests in CI
					List<String> names = StreamSupport.stream(models.spliterator(), false).map(jsonNode -> jsonNode.get("name").asText()).collect(Collectors.toList());
					int idx = names.indexOf("My Hub Model");
					assertEquals("My Hub Model", models.get(idx).get("name").asText());
					idx = names.indexOf("Test Model");
					JSONAssert.assertEquals(getResource("models/model.json"), objectMapper.writeValueAsString(models.get(idx)), true);
				})
			.andExpect(status().isOk());
	}

	@Test
	void deleteModelMultiTenant() throws Exception {
		envisionConfig.setMultiTenant(true);
		File modelDir = modelService.getModelsDir(true, ACCOUNT_NAME);
		assertEquals(0, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		registerAccount();
		registerAccount(ACCOUNT_NAME2, ACCOUNT_PASSWORD);

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		postJson(DELETE_MODEL_URL, readJsonObject("{\"name\": \"Test Model\"}"))
			.andExpect(status().isUnauthorized());

		login();
		postJson(DELETE_MODEL_URL, readJsonObject("{\"name\": \"Test Model\"}"))
			.andExpect(status().isNotFound());

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/model.json"));

		assertEquals(2, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(5, getEntityCount());
		assertEquals(5, getEntityManager().getEntities().size());

		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(2, models.size());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);
					JSONAssert.assertEquals(getResource("models/model.json"), objectMapper.writeValueAsString(models.get(1)), true);
				})
			.andExpect(status().isOk());

		postJson(DELETE_MODEL_URL, readJsonObject("{\"name\": \"Test Model\"}"))
			.andExpect(status().isNoContent());

		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(1, models.size());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);
				})
			.andExpect(status().isOk());
		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());
	}

	@Test
	void deleteModelSingleTenant() throws Exception {
		envisionConfig.setMultiTenant(false);
		File modelDir = modelService.getModelsDir(false, ACCOUNT_NAME);
		assertEquals(0, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		registerAccount();
		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		postJson(DELETE_MODEL_URL, readJsonObject("{\"name\": \"Test Model\"}"))
			.andExpect(status().isUnauthorized());

		login();
		postJson(DELETE_MODEL_URL, readJsonObject("{\"name\": \"Test Model\"}"))
			.andExpect(status().isNotFound());

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/model.json"));

		assertEquals(2, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(5, getEntityCount());
		assertEquals(5, getEntityManager().getEntities().size());

		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(2, models.size());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);
					JSONAssert.assertEquals(getResource("models/model.json"), objectMapper.writeValueAsString(models.get(1)), true);
				})
			.andExpect(status().isOk());

		postJson(DELETE_MODEL_URL, readJsonObject("{\"name\": \"Test Model\"}"))
			.andExpect(status().isNoContent());

		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(1, models.size());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);
				})
			.andExpect(status().isOk());
		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());
	}

	@Test
	void saveModelMultiTenant() throws Exception {
		envisionConfig.setMultiTenant(true);
		installEnvisionModules();
		File modelDir = modelService.getModelsDir(true, ACCOUNT_NAME);
		assertEquals(0, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		registerAccount();
		registerAccount(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		putJson(SAVE_MODEL_URL, readJsonObject("{\"name\":\"My Empty Model\",\"edges\":{},\"nodes\":{}}"))
			.andExpect(status().isUnauthorized());

		loginAsUser(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
		putJson(SAVE_MODEL_URL, readJsonObject("{\"name\":\"My Empty Model\",\"edges\":{},\"nodes\":{}}"))
			.andExpect(status().isNoContent());

		assertEquals(0, getEntityCount());

		putJson(SAVE_MODEL_URL, readJsonObject(getResourceStream("models/model.json")))
			.andExpect(status().isNoContent());

		assertEquals(5, getEntityCount());
		assertEquals(5, getDocCount(getAdminHubClient().getFinalSchemasClient(), "ml-data-hub-xml-schema"));
		assertEquals(5, getDocCount(getAdminHubClient().getFinalSchemasClient(), "ml-data-hub-json-schema"));
		assertEquals(5, getDocCount(getAdminHubClient().getFinalSchemasClient(), "http://marklogic.com/entity-services/models"));
		logout();

		login();

		putJson(SAVE_MODEL_URL, readJsonObject("{\"name\":\"My Empty Model\",\"edges\":{},\"nodes\":{}}"))
			.andExpect(status().isNoContent());

		assertEquals(2, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(5, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		putJson(SAVE_MODEL_URL, readJsonObject(getResourceStream("models/model.json")))
			.andExpect(status().isNoContent());
		assertEquals(3, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(10, getEntityCount());
		assertEquals(5, getEntityManager().getEntities().size());
		String[] uris = StreamSupport.stream(getEntities().spliterator(), false)
			.map(jsonNode -> jsonNode.get("uri").asText())
			.sorted().toArray(String[]::new);
		assertEquals("/entities/bob.smith@marklogic.com/Department.entity.json", uris[0]);
		assertEquals("/entities/bob.smith@marklogic.com/Employee.entity.json", uris[1]);
		assertEquals("/entities/bob.smith@marklogic.com/MegaCorp.entity.json", uris[2]);
		assertEquals("/entities/bob.smith@marklogic.com/Organization.entity.json", uris[3]);
		assertEquals("/entities/bob.smith@marklogic.com/Planet.entity.json", uris[4]);

		putJson(SAVE_MODEL_URL, readJsonObject(getResourceStream("models/MyWackyModel.json")))
			.andExpect(status().isNoContent());

		assertEquals(4, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(7, getEntityCount());
		assertEquals(2, getEntityManager().getEntities().size());
		uris = StreamSupport.stream(getEntities().spliterator(), false)
			.map(jsonNode -> jsonNode.get("uri").asText())
			.sorted().toArray(String[]::new);
		assertEquals("/entities/bob.smith@marklogic.com/Department.entity.json", uris[0]);
		assertEquals("/entities/bob.smith@marklogic.com/Employee.entity.json", uris[1]);

		logout();
		loginAsUser(ACCOUNT_NAME2, ACCOUNT_PASSWORD);

		assertEquals(4, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(7, getEntityCount());
		assertEquals(5, getEntityManager(ACCOUNT_NAME2, ACCOUNT_PASSWORD).getEntities().size());
		uris = StreamSupport.stream(getEntities().spliterator(), false)
			.map(jsonNode -> jsonNode.get("uri").asText())
			.sorted().toArray(String[]::new);
		assertEquals("/entities/bob.smith@marklogic.com/Department.entity.json", uris[0]);
		assertEquals("/entities/bob.smith@marklogic.com/Employee.entity.json", uris[1]);
		assertEquals("/entities/jim.jones@marklogic.com/Department.entity.json", uris[2]);
		assertEquals("/entities/jim.jones@marklogic.com/Employee.entity.json", uris[3]);
		assertEquals("/entities/jim.jones@marklogic.com/MegaCorp.entity.json", uris[4]);
		assertEquals("/entities/jim.jones@marklogic.com/Organization.entity.json", uris[5]);
		assertEquals("/entities/jim.jones@marklogic.com/Planet.entity.json", uris[6]);
	}

	@Test
	void saveModelSingleTenant() throws Exception {
		envisionConfig.setMultiTenant(false);
		File modelDir = modelService.getModelsDir(false, ACCOUNT_NAME);
		assertEquals(0, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		registerAccount();
		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		login();

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);

		putJson(SAVE_MODEL_URL, readJsonObject("{\"name\":\"My Empty Model\",\"edges\":{},\"nodes\":{}}"))
			.andExpect(status().isNoContent());

		assertEquals(2, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		putJson(SAVE_MODEL_URL, readJsonObject(getResourceStream("models/model.json")))
			.andExpect(status().isNoContent());
		assertEquals(3, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(5, getEntityCount());
		assertEquals(5, getEntityManager().getEntities().size());
		String[] uris = StreamSupport.stream(getEntities().spliterator(), false)
			.map(jsonNode -> jsonNode.get("uri").asText())
			.sorted().toArray(String[]::new);
		assertEquals("/entities/Department.entity.json", uris[0]);
		assertEquals("/entities/Employee.entity.json", uris[1]);
		assertEquals("/entities/MegaCorp.entity.json", uris[2]);
		assertEquals("/entities/Organization.entity.json", uris[3]);
		assertEquals("/entities/Planet.entity.json", uris[4]);

		putJson(SAVE_MODEL_URL, readJsonObject(getResourceStream("models/MyWackyModel.json")))
			.andExpect(status().isNoContent());

		assertEquals(4, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(2, getEntityCount());
		assertEquals(2, getEntityManager().getEntities().size());
		uris = StreamSupport.stream(getEntities().spliterator(), false)
			.map(jsonNode -> jsonNode.get("uri").asText())
			.sorted().toArray(String[]::new);
		assertEquals("/entities/Department.entity.json", uris[0]);
		assertEquals("/entities/Employee.entity.json", uris[1]);
	}

	@Test
	void importModel() throws Exception {
		File modelDir = modelService.getModelsDir(false, ACCOUNT_NAME);
		assertEquals(0, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		registerAccount();
		removeModelFiles(false, ACCOUNT_NAME);
		removeEntityFiles();

		assertEquals(0, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		putJson(IMPORT_MODEL_URL, readJsonObject("{}"))
			.andExpect(status().isUnauthorized());

		assertEquals(0, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		login();

		putJson(IMPORT_MODEL_URL, readJsonObject("{}"))
			.andExpect(status().isNoContent());

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		removeModelFiles(false, ACCOUNT_NAME);
		removeEntityFiles();

		assertEquals(0, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, getEntityManager().getEntities().size());

		getEntityManager().saveEntity(HubEntity.fromJson("Planet.entity.json", readJsonObject(getResourceStream("esEntities/Planet.entity.json"))), false);
		deployService.deployEntities(getNonAdminHubClient());

		assertEquals(0, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(1, getEntityCount());
		assertEquals(1, getEntityManager().getEntities().size());

		putJson(IMPORT_MODEL_URL, readJsonObject("{}"))
			.andExpect(status().isNoContent());

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals(1, getEntityCount());
		assertEquals(1, getEntityManager().getEntities().size());

		JsonNode model = modelService.getModel(getNonAdminHubClient().getFinalClient());
		JsonNode nodes = model.get("nodes");
		assertEquals(1, nodes.size());
		assertEquals("Planet", nodes.get("planet").get("entityName").asText());
	}

	@Test
	void testActiveIndexes() throws Exception {
		HubConfigImpl hubConfig = getNonAdminHubClient().getHubConfig();
		SimpleAppDeployer roleDeployer = new SimpleAppDeployer(hubConfig.getManageClient(), hubConfig.getAdminManager());
		roleDeployer.getCommands().clear();
		roleDeployer.getCommands().add(new DeployHubDatabaseCommand(hubConfig, getResourceFile("final-database.json"), "final-database.json"));
		roleDeployer.deploy(hubConfig.getAppConfig());

		getJson(GET_ACTIVE_INDEXES_URL)
			.andExpect(status().isUnauthorized());

		registerAccount();
		login();

		getJson(GET_ACTIVE_INDEXES_URL)
			.andDo(
				result -> {
					ArrayNode node = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(0, node.size());
				})
			.andExpect(status().isOk());

		roleDeployer.getCommands().clear();
		roleDeployer.getCommands().add(new DeployHubDatabaseCommand(hubConfig, getResourceFile("final-database-with-indexes.json"), "final-database.json"));
		roleDeployer.deploy(hubConfig.getAppConfig());

		getJson(GET_ACTIVE_INDEXES_URL)
			.andDo(
				result -> {
					ArrayNode node = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(2, node.size());
					assertEquals("entityConfigFinal1", node.get(0).asText());
					assertEquals("entityConfigFinal2", node.get(1).asText());
				})
			.andExpect(status().isOk());

		roleDeployer.getCommands().clear();
		roleDeployer.getCommands().add(new DeployHubDatabaseCommand(hubConfig, getResourceFile("final-database.json"), "final-database.json"));
		roleDeployer.deploy(hubConfig.getAppConfig());
	}

	@Test
	void renameModel() throws Exception {
		postJson(RENAME_MODEL_URL, readJsonObject("{\"originalname\": \"My Model\", \"model\": {\"name\":\"My Updated Model\",\"edges\":{},\"nodes\":{} } }"))
			.andExpect(status().isUnauthorized());
		registerAccount();
		login();

		File modelDir = modelService.getModelsDir(false, ACCOUNT_NAME);
		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals("MyModel.json", Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json")))[0].getName());

		postJson(RENAME_MODEL_URL, readJsonObject("{\"originalname\": \"My Model\", \"model\": {\"name\":\"My Updated Model\",\"edges\":{},\"nodes\":{} } }"))
			.andExpect(status().isNoContent());

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json"))).length);
		assertEquals("MyUpdatedModel.json", Objects.requireNonNull(modelDir.listFiles((dir, name) -> name.endsWith("json")))[0].getName());

		assertEquals("My Updated Model", modelService.getModel(getNonAdminHubClient().getFinalClient()).get("name").asText());
	}
}
