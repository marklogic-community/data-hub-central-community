package com.marklogic.envision.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.envision.config.EnvisionConfig;
import com.marklogic.envision.dataServices.EntityModeller;
import com.marklogic.envision.deploy.DeployService;
import com.marklogic.envision.entities.EntityManagerService;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.hub.EntityManager;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.entity.HubEntity;
import com.marklogic.hub.impl.EntityManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

@Service
public class ModelService {
	private final EnvisionConfig envisionConfig;
	private final DeployService deployService;
	private final EntityManagerService entityManagerService;

    @Autowired
	ModelService(EnvisionConfig envisionConfig, DeployService deployService, EntityManagerService entityManagerService) {
    	this.envisionConfig = envisionConfig;
    	this.deployService = deployService;
    	this.entityManagerService = entityManagerService;
	}

	EntityManager getEntityManager(HubClient hubClient) {
		return new EntityManagerImpl(hubClient.getHubConfig());
	}

	@Value("${modelsDir:conceptConnectorModels}")
	private Path modelsDir;

    private File modelsDirFile = null;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@PostConstruct
	public void postConstruct() {
		getModelsDirFile();
	}

	public void setModelsDir(Path modelsDir) {
		this.modelsDir = modelsDir;
		clearCachedModelsDirFile();
	}

	public void clearCachedModelsDirFile() {
		// clear out cached modelsDirFile
		this.modelsDirFile = null;
	}

	private File getModelsDirFile() {
		if (this.modelsDirFile == null) {
			try {
				this.modelsDirFile = envisionConfig.dhfDir.toPath().resolve(modelsDir).toAbsolutePath().toFile();
				if (!modelsDirFile.exists()) {
					modelsDirFile.mkdirs();
				}
				System.out.println("modelsDir: " + modelsDirFile.getAbsolutePath());
			}
			catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
		return this.modelsDirFile;
	}

	public File getModelsDir(boolean isMultiTenant, String username) {
		File modelsDirFile = getModelsDirFile();
		if (isMultiTenant) {
			File userModelsDir = new File(modelsDirFile, username);
			if (!userModelsDir.exists()) {
				userModelsDir.mkdirs();
			}
			return userModelsDir;
		}
		return modelsDirFile;
	}

	private File getModelFile(boolean isMultiTenant, String username, String modelName) {
		File userModelDir = getModelsDir(isMultiTenant, username);
		String fileName = modelName.replace(" ", "") + ".json";
		return new File(userModelDir, fileName);
	}

    public void toDataHub(HubClient hubClient, JsonNode existingEntities) {
        JsonNode newEntities = EntityModeller.on(hubClient.getFinalClient()).toDatahub();

        List<String> fieldNames = new ArrayList<>();
        newEntities.fieldNames().forEachRemaining(entityName -> {
			fieldNames.add(entityName);
            String entityFileName = entityName + ".entity.json";
            JsonNode hubEntityJSON = newEntities.get(entityName);
            HubEntity hubEntity = HubEntity.fromJson(entityFileName, hubEntityJSON);

			EntityManager em = getEntityManager(hubClient);
			try {
				em.saveEntity(hubEntity, false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			Path protectedPaths = hubClient.getHubConfig().getUserSecurityDir().resolve("protected-paths");
			File[] files = protectedPaths.toFile().listFiles((dir, name) -> name.endsWith(HubConfig.PII_PROTECTED_PATHS_FILE));
			if (files != null) {
				for (File f : files) {
					f.delete();
				}
			}
			em.savePii();
        });

        EntityModeller.on(hubClient.getFinalClient()).removeAllEntities(hubClient.isMultiTenant() ?  hubClient.getUsername() : null);
		deleteExtraEntities(hubClient, fieldNames);
		EntityModeller.on(hubClient.getFinalClient()).updateRedaction();
		EntityModeller.on(hubClient .getFinalClient()).updatePii(existingEntities, newEntities);
		deployService.deployEntities(hubClient);
    }

    public void deleteAllModels(HubClient hubClient, String username) throws IOException {
		File userModelDir = getModelsDir(hubClient.isMultiTenant(), username);
		File[] files = userModelDir.listFiles();
		for (File file : files) {
			JsonNode model = objectMapper.readTree(file);
			String modelName = model.get("name").asText();
			deleteModel(hubClient, username, modelName);
		}
	}

    public boolean deleteModel(HubClient hubClient, String username, String modelName) throws IOException {
		File jsonFile = getModelFile(hubClient.isMultiTenant(), username, modelName);
		if (jsonFile.exists()) {
			JsonNode model = objectMapper.readTree(jsonFile);
			model.get("nodes").forEach(jsonNode -> {
				try {
					String entityName = jsonNode.get("entityName").asText();
					getEntityManager(hubClient).deleteEntity(entityName);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

			EntityModeller.on(hubClient.getFinalClient()).removeAllEntities(hubClient.isMultiTenant() ? username : null);
		}
		return jsonFile.delete();
	}

	public void renameModel(String username, HubClient client, InputStream stream) throws IOException {
    	JsonNode node = objectMapper.readTree(stream);
		String originalModelName = node.get("originalname").asText();
		File originalModelFile = getModelFile(client.isMultiTenant(), username, originalModelName);

		saveModel(client, node.get("model"));
		originalModelFile.delete();
	}

	private void deleteExtraEntities(HubClient hubClient, List<String> legitEntities) {
		 entityManagerService.getEntities(hubClient).forEach(hubEntity -> {
			String entityName = hubEntity.getInfo().getTitle();
			if (!legitEntities.contains(entityName.toLowerCase())) {
				// attempt delete from server
				try {
					entityManagerService.deleteEntity(hubClient, entityName);
				} catch (FailedRequestException e) {}
				// attempt to delete from project folder
				try {
					getEntityManager(hubClient).deleteEntity(entityName);
				} catch (IOException e) {}
			}
		});
	}

	public void importModel(HubClient client) throws IOException {
		JsonNode node = EntityModeller.on(client.getFinalClient()).fromDatahub();
		saveModel(client, node);
	}

	public void saveModel(HubClient client, InputStream stream) throws IOException {
		JsonNode node = objectMapper.readTree(stream);
		saveModel(client, node);
	}

	private void saveModel(HubClient client, JsonNode model) throws IOException {
		JsonNode existingEntities = EntityModeller.on(client.getFinalClient()).toDatahub();
		DocumentMetadataHandle meta = new DocumentMetadataHandle();
		meta.getCollections().addAll("http://marklogic.com/envision/model");
		JacksonHandle content = new JacksonHandle(model);
		client.getFinalClient().newJSONDocumentManager().write("/envision/" + client.getUsername() + "/currentModel.json", meta, content);
		saveModelFile(client.isMultiTenant(), client.getUsername(), model);

		toDataHub(client, existingEntities);
		createModelTDEs(client.getFinalClient());
	}

	public void saveModelFile(boolean isMultiTenant, String username, JsonNode model) throws IOException {
		File jsonFile = getModelFile(isMultiTenant, username, model.get("name").asText());
		objectMapper.writeValue(jsonFile, model);
	}

    public void createModelTDEs(DatabaseClient client) {
        EntityModeller.on(client).createTdes();
	}

	public JsonNode getModel(DatabaseClient client) {
    	return EntityModeller.on(client).getCurrentModel();
	}

	public List<JsonNode> getAllModels(HubClient client, String username) throws IOException {
		List<JsonNode> names = listAllModels(client.isMultiTenant(), username);

		ArrayNode models = objectMapper.convertValue(names, ArrayNode.class);
		if (EntityModeller.on(client.getFinalClient()).needsImport(models)) {
			this.importModel(client);
			names = listAllModels(client.isMultiTenant(), username);
		}
		return names;
	}

	public JsonNode getActiveIndexes(DatabaseClient client) {
		return EntityModeller.on(client).getActiveIndexes();
	}

	private List<JsonNode> listAllModels(boolean isMultiTenant, String username) {
		List<JsonNode> names = new ArrayList<>();
		File modelsDir = getModelsDir(isMultiTenant, username);
		File[] modelFiles = modelsDir.listFiles(pathname -> pathname.toString().endsWith(".json"));
		if (modelFiles != null) {
			try {
				for (File modelFile: modelFiles) {
					FileInputStream fileInputStream = new FileInputStream(modelFile);
					JsonNode node = objectMapper.readTree(fileInputStream);
					names.add(updateLegacyModel(node));
					fileInputStream.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return names;
	}

	/**
	 * Converts Models to latest format
	 * // v1: convert property types to proper xsd types
	 * @param model - the model to convert
	 * @return returns the converted model
	 */
	private JsonNode updateLegacyModel(JsonNode model) {
    	JsonNode nodes = model.get("nodes");
		List<String> oldTypes = new ArrayList<>(Arrays.asList("String", "Boolean", "Integer", "Decimal", "Date"));
    	if (nodes.isObject()) {
			Iterator<Map.Entry<String, JsonNode>> iterator = nodes.fields();
			while (iterator.hasNext()) {
				Map.Entry<String, JsonNode> kv = iterator.next();
				JsonNode properties = kv.getValue().get("properties");
				if (properties.isArray()) {
					Iterator<JsonNode> propsIterator = properties.elements();
					while (propsIterator.hasNext()) {
						JsonNode propNode = propsIterator.next();
						if (propNode.isObject()) {
							ObjectNode prop = (ObjectNode)propNode;
							String type = prop.get("type").asText();
							String newType = null;
							if (oldTypes.contains(type)) {
								newType = type.toLowerCase();
							}
							if (newType != null) {
								prop.set("type", new TextNode(newType));
							}
						}
					}
				}
			}
		}
		return model;
	}
}
