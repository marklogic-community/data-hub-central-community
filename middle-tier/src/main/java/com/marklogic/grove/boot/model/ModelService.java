package com.marklogic.grove.boot.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.hub.EntityManager;
import com.marklogic.hub.entity.HubEntity;
import com.marklogic.envision.EntityModeller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ModelService {

    @Autowired
    EntityManager em;

	@Value("${modelsDir}")
	public void setModelsDir(File modelsDir) {
		try {
			this.modelsDir = modelsDir.getCanonicalFile();
			System.out.println("modelsDir: " + this.modelsDir.toString());
		}
		catch(Exception e) {}
		if (!modelsDir.exists()) {
			modelsDir.mkdirs();
		}
	}

	private File modelsDir;


	private ObjectMapper objectMapper = new ObjectMapper();

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
		deleteExtraHubentities(fieldNames);
    }

    public void deleteModel(DatabaseClient client, InputStream stream) throws IOException {
		JsonNode node = objectMapper.readTree(stream);
		String fileName = node.get("name").asText().replace(" ", "") + ".json";
		File jsonFile = new File(modelsDir, fileName);
		jsonFile.delete();
	}

	private void deleteAllHubEntities() {
		// delete all entities
		em.getEntities().forEach(hubEntity -> {
			try {
				em.deleteEntity(hubEntity.getInfo().getTitle());
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
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

	public List<JsonNode> getAllModels(DatabaseClient client) throws IOException {
		List<JsonNode> names = listAllModels(client);

		ArrayNode models = objectMapper.convertValue(names, ArrayNode.class);
		if (EntityModeller.on(client).needsImport(models)) {
			this.importModel(client);
			names = listAllModels(client);
		}
		return names;
	}

	private List<JsonNode> listAllModels(DatabaseClient client) throws IOException {
		List<JsonNode> names = new ArrayList<>();
		File[] modelFiles = modelsDir.listFiles(pathname -> pathname.toString().endsWith(".json"));
		if (modelFiles!= null) {
			try {
				for (File modelFile: modelFiles) {
					FileInputStream fileInputStream = new FileInputStream(modelFile);
					JsonNode node = objectMapper.readTree(fileInputStream);
					names.add(node);
					fileInputStream.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return names;
	}
}
