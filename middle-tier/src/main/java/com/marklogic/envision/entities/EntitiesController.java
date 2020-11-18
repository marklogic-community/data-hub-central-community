package com.marklogic.envision.entities;

import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.entity.HubEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entities")
public class EntitiesController extends AbstractController {
	final private EntityManagerService entityManagerService;

	@Autowired
	EntitiesController(EntityManagerService entityManagerService) {
		this.entityManagerService = entityManagerService;
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public List<HubEntity> getEntities() {
		return entityManagerService.getEntities(getHubClient());
	}

	@RequestMapping(value = "/{entityName}", method = RequestMethod.GET)
	@ResponseBody
	public HubEntity getEntity(@PathVariable String entityName, @RequestParam(required = false)Boolean extendSubEntities) {
		boolean extSubEntities = (extendSubEntities != null) && extendSubEntities;
		return entityManagerService.getEntity(getHubClient(), entityName, extSubEntities);
	}
}
