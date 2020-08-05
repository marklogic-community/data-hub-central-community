package com.marklogic.envision.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.envision.dataServices.EntityModeller;
import com.marklogic.envision.deploy.DeployService;
import com.marklogic.grove.boot.error.NotFoundException;
import com.marklogic.hub.EntityManager;
import com.marklogic.hub.entity.HubEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ModelService {

    private final EntityManager em;

    private final DeployService deployService;

    @Autowired
	ModelService(EntityManager em, DeployService deployService) {
    	this.em = em;
    	this.deployService = deployService;
	}

	@Value("${modelsDir}")
	public void setModelsDir(File modelsDir) {
		try {
			this.modelsDir = modelsDir.getCanonicalFile();
			System.out.println("modelsDir: " + this.modelsDir.toString());
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
		if (!modelsDir.exists()) {
			modelsDir.mkdirs();
		}
	}

	private File modelsDir;

	private final ObjectMapper objectMapper = new ObjectMapper();

    public void toDataHub(DatabaseClient client ) {
        JsonNode node = EntityModeller.on(client).toDatahub();
        List<String> fieldNames = new ArrayList<>();
        node.fieldNames().forEachRemaining(entityName -> {
			fieldNames.add(entityName);
            String entityFileName = entityName + ".entity.json";
            HubEntity hubEntity = HubEntity.fromJson(entityFileName, node.get(entityName));

            try {
                em.saveEntity(hubEntity, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        EntityModeller.on(client).removeAllEntities();
		deployService.deployEntities();
		deleteExtraHubentities(fieldNames);
    }

    public void deleteModel(InputStream stream) throws IOException {
		JsonNode node = objectMapper.readTree(stream);
		String fileName = node.get("name").asText().replace(" ", "") + ".json";
		File jsonFile = new File(modelsDir, fileName);
		jsonFile.delete();
	}

	public void renameModel(DatabaseClient client, InputStream stream) throws IOException {
    	JsonNode node = objectMapper.readTree(stream);
		String originalModelName = node.get("originalname").asText().replace(" ", "") + ".json";

		File originalModelFile = new File(modelsDir, originalModelName);

		saveModel(client, node.get("model"));
		originalModelFile.delete();
	}

	private void deleteExtraHubentities(List<String> legitEntities) {
		em.getEntities().forEach(hubEntity -> {
			try {
				String entityName = hubEntity.getInfo().getTitle();
				if (!legitEntities.contains(entityName.toLowerCase())) {
					em.deleteEntity(entityName);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public void importModel(DatabaseClient client) throws IOException {
		JsonNode node = EntityModeller.on(client).fromDatahub();
		saveModel(client, node);
	}

	public void saveModel(DatabaseClient client, InputStream stream) throws IOException {
		JsonNode node = objectMapper.readTree(stream);
		saveModel(client, node);
	}

	private void saveModel(DatabaseClient client, JsonNode node) throws IOException {
		DocumentMetadataHandle meta = new DocumentMetadataHandle();

		JacksonHandle content = new JacksonHandle(node);

		client.newJSONDocumentManager().write("model.json", meta, content);
		toDataHub(client);
		createModelTDEs(client);

		String fileName = node.get("name").asText().replace(" ", "") + ".json";
		File jsonFile = new File(modelsDir, fileName);
		objectMapper.writeValue(jsonFile, node);
	}

    public void createModelTDEs(DatabaseClient client) {
        EntityModeller.on(client).createTdes();
	}

	public JsonNode getModel(DatabaseClient client, String modelName) {
		DocumentPage page = client.newDocumentManager().read(modelName);
		if (!page.hasNext()) {
			throw new NotFoundException();
		}
		DocumentRecord documentRecord = page.next();
		JsonNode model = documentRecord.getContent(new JacksonHandle()).get();
		return updateLegacyModel(model);
	}
	public List<JsonNode> getAllModels(DatabaseClient client) throws IOException {
		List<JsonNode> names = listAllModels();

		ArrayNode models = objectMapper.convertValue(names, ArrayNode.class);
		if (EntityModeller.on(client).needsImport(models)) {
			this.importModel(client);
			names = listAllModels();
		}
		return names;
	}

	public JsonNode getActiveIndexes(DatabaseClient client) {
		return EntityModeller.on(client).getActiveIndexes();
	}

	private List<JsonNode> listAllModels() {
		List<JsonNode> names = new ArrayList<>();
		File[] modelFiles = modelsDir.listFiles(pathname -> pathname.toString().endsWith(".json"));
		if (modelFiles!= null) {
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
