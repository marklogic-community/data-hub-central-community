package com.marklogic.envision.entities;

import com.marklogic.hub.EntityManager;
import com.marklogic.hub.entity.HubEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntityManagerService {
	private final EntityManager em;

	@Autowired
	EntityManagerService(EntityManager em) {
		this.em = em;
	}

	public List<HubEntity> getEntities() {
		return em.getEntities();
	}

	public HubEntity getEntity(String entityName, Boolean extendSubEntities) {
		return em.getEntityFromProject(entityName, extendSubEntities);
	}
}
