package com.marklogic.envision.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.andrewoma.dexx.collection.Iterable;
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
		return getEntityManager(hubClient) .getEntityFromProject(entityName, extendSubEntities);
	}

	private ModelsService newService(HubClient hubClient) {
		return ModelsService.on(hubClient.getStagingClient());
	}

	private EntityManager getEntityManager(HubClient hubClient) {
		return new EntityManagerImpl(hubClient.getHubConfig());
	}
}
