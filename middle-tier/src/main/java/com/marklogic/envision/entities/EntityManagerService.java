package com.marklogic.envision.entities;

import com.marklogic.envision.hub.HubClient;
import com.marklogic.hub.EntityManager;
import com.marklogic.hub.entity.HubEntity;
import com.marklogic.hub.impl.EntityManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntityManagerService {
	EntityManager getEntityManager(HubClient hubClient) {
		return new EntityManagerImpl(hubClient.getHubConfig());
	}

	public List<HubEntity> getEntities(HubClient hubClient) {
		return getEntityManager(hubClient).getEntities();
	}

	public HubEntity getEntity(HubClient hubClient, String entityName, Boolean extendSubEntities) {
		return getEntityManager(hubClient).getEntityFromProject(entityName, extendSubEntities);
	}
}
