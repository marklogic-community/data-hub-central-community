package com.marklogic.envision.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.andrewoma.dexx.collection.Iterable;
import com.marklogic.client.FailedRequestException;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.hub.EntityManager;
import com.marklogic.hub.dataservices.ModelsService;
import com.marklogic.hub.entity.HubEntity;
import com.marklogic.hub.impl.EntityManagerImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class EntityManagerService {
	public List<HubEntity> getEntities(HubClient hubClient) {
		List<HubEntity> hubEntites = new ArrayList<HubEntity>();
		Iterator<JsonNode> models = newService(hubClient).getPrimaryEntityTypes().elements();
		while (models.hasNext()) {
			JsonNode model = models.next();
			hubEntites.add(HubEntity.fromJson(model.path("info").path("title").asText(""), model.path("model")));
		}
		return hubEntites;
	}

	public HubEntity getEntity(HubClient hubClient, String entityName, Boolean extendSubEntities) {
		ModelsService service = newService(hubClient);
		Iterator<JsonNode> models = service.getPrimaryEntityTypes().elements();
		while (models.hasNext()) {
			JsonNode model = models.next();
			String modelTitle = model.path("info").path("title").asText("");
			if (modelTitle.equals(entityName)) {
				return HubEntity.fromJson(modelTitle, model.path("model"));
			}
		}
		// Not found in the server, try the project
		EntityManager em = new EntityManagerImpl(hubClient.getHubConfig());
		HubEntity hubEntity = em.getEntityFromProject(entityName, extendSubEntities);
		if (hubEntity != null) {
			try {
				service.saveModel(hubEntity.toJson());
			} catch (FailedRequestException e) {}
		}
		return hubEntity;
	}

	public void deleteEntity(HubClient hubClient, String entityName) {
		newService(hubClient).deleteModel(entityName);
	}

	private ModelsService newService(HubClient hubClient) {
		return ModelsService.on(hubClient.getStagingClient());
	}

	private EntityManager newEntityManager(HubClient hubClient) {
		return new EntityManagerImpl(hubClient.getHubConfig());
	}

}
