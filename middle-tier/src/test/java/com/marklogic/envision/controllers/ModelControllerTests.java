package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.envision.deploy.DeployService;
import com.marklogic.envision.model.ModelService;
import com.marklogic.hub.EntityManager;
import com.marklogic.hub.deploy.commands.DeployHubDatabaseCommand;
import com.marklogic.hub.entity.HubEntity;
import com.marklogic.hub.impl.HubConfigImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
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
	EntityManager em;

	@Autowired
	DeployService deployService;

	protected void removeModelFiles() {
		File modelDir = modelService.getModelsDir(ACCOUNT_NAME);
		for (File file: Objects.requireNonNull(modelDir.listFiles())) {
			file.delete();
		}
	}

	protected void removeEntityFiles() throws IOException {
		for (HubEntity entity : em.getEntities()) {
			em.deleteEntity(entity.getInfo().getTitle());
		}
	}

	@BeforeEach
	void setup() throws IOException {
		logout();

		// remove models
		removeModelFiles();
		removeEntityFiles();

		removeUser(ACCOUNT_NAME);
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
	void getAllModels() throws Exception {
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

		em.saveEntity(HubEntity.fromJson("Orphan.entity.json", readJsonObject(getResourceStream("esEntities/Orphan.entity.json"))), false);
		deployService.deployEntities(getAdminHubClient());

		getJson(ALL_MODELS_URL)
			.andDo(
				result -> {
					ArrayNode models = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(3, models.size());
					JSONAssert.assertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", objectMapper.writeValueAsString(models.get(0)), true);
					assertEquals("My Hub Model", models.get(1).get("name").asText());
					JSONAssert.assertEquals(getResource("models/model.json"), objectMapper.writeValueAsString(models.get(2)), true);
				})
			.andExpect(status().isOk());
	}

	@Test
	void deleteModel() throws Exception {
		File modelDir = modelService.getModelsDir(ACCOUNT_NAME);
		assertEquals(0, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		registerAccount();
		assertEquals(1, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		postJson(DELETE_MODEL_URL, readJsonObject("{\"name\": \"Test Model\"}"))
			.andExpect(status().isUnauthorized());

		login();
		postJson(DELETE_MODEL_URL, readJsonObject("{\"name\": \"Test Model\"}"))
			.andExpect(status().isNotFound());

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/model.json"));

		assertEquals(2, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(5, getEntityCount());
		assertEquals(5, em.getEntities().size());

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
		assertEquals(1, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());
	}

	@Test
	void saveModel() throws Exception {
		File modelDir = modelService.getModelsDir(ACCOUNT_NAME);
		assertEquals(0, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		registerAccount();
		assertEquals(1, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		putJson(SAVE_MODEL_URL, readJsonObject("{\"name\":\"My Empty Model\",\"edges\":{},\"nodes\":{}}"))
			.andExpect(status().isUnauthorized());

		login();

		putJson(SAVE_MODEL_URL, readJsonObject("{\"name\":\"My Empty Model\",\"edges\":{},\"nodes\":{}}"))
			.andExpect(status().isNoContent());

		assertEquals(2, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		putJson(SAVE_MODEL_URL, readJsonObject(getResourceStream("models/model.json")))
			.andExpect(status().isNoContent());
		assertEquals(3, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(5, getEntityCount());
		assertEquals(5, em.getEntities().size());
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

		assertEquals(4, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(2, getEntityCount());
		assertEquals(2, em.getEntities().size());
		uris = StreamSupport.stream(getEntities().spliterator(), false)
			.map(jsonNode -> jsonNode.get("uri").asText())
			.sorted().toArray(String[]::new);
		assertEquals("/entities/Department.entity.json", uris[0]);
		assertEquals("/entities/Employee.entity.json", uris[1]);
	}

	@Test
	void importModel() throws Exception {
		File modelDir = modelService.getModelsDir(ACCOUNT_NAME);
		assertEquals(0, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		registerAccount();
		removeModelFiles();
		removeEntityFiles();

		assertEquals(0, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		putJson(IMPORT_MODEL_URL, readJsonObject("{}"))
			.andExpect(status().isUnauthorized());

		assertEquals(0, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		login();

		putJson(IMPORT_MODEL_URL, readJsonObject("{}"))
			.andExpect(status().isNoContent());

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		removeModelFiles();
		removeEntityFiles();

		assertEquals(0, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(0, getEntityCount());
		assertEquals(0, em.getEntities().size());

		em.saveEntity(HubEntity.fromJson("Planet.entity.json", readJsonObject(getResourceStream("esEntities/Planet.entity.json"))), false);
		deployService.deployEntities(getAdminHubClient());

		assertEquals(0, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(1, getEntityCount());
		assertEquals(1, em.getEntities().size());

		putJson(IMPORT_MODEL_URL, readJsonObject("{}"))
			.andExpect(status().isNoContent());

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals(1, getEntityCount());
		assertEquals(1, em.getEntities().size());

		JsonNode model = modelService.getModel(getNonAdminHubClient().getFinalClient());
		JsonNode nodes = model.get("nodes");
		assertEquals(1, nodes.size());
		assertEquals("Planet", nodes.get("planet").get("entityName").asText());
	}

	@Test
	void testActiveIndexes() throws Exception {
		HubConfigImpl hubConfig = getHubConfig();
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

		File modelDir = modelService.getModelsDir(ACCOUNT_NAME);
		assertEquals(1, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals("MyModel.json", Objects.requireNonNull(modelDir.listFiles())[0].getName());

		postJson(RENAME_MODEL_URL, readJsonObject("{\"originalname\": \"My Model\", \"model\": {\"name\":\"My Updated Model\",\"edges\":{},\"nodes\":{} } }"))
			.andExpect(status().isNoContent());

		assertEquals(1, Objects.requireNonNull(modelDir.listFiles()).length);
		assertEquals("MyUpdatedModel.json", Objects.requireNonNull(modelDir.listFiles())[0].getName());

		assertEquals("My Updated Model", modelService.getModel(getNonAdminHubClient().getFinalClient()).get("name").asText());
	}
}
