package com.marklogic.grove.boot.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.impl.HubConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/search")
public class SearchController extends AbstractController {

	final private SearchService searchService;

	@Autowired
	SearchController(HubConfigImpl hubConfig, SearchService searchService) {
		super(hubConfig);
		this.searchService = searchService;
	}

	@RequestMapping(value = "/{type}", method = RequestMethod.POST)
	public JsonNode search(@PathVariable String type, @RequestBody ObjectNode searchRequest, HttpSession session) {
		long start = 1;
		long pageLength = 10;
		if (searchRequest.has("options")) {
			ObjectNode options = (ObjectNode) searchRequest.get("options");
			if (options.has("start")) {
				start = options.get("start").asLong();
			}
			if (options.has("pageLength")) {
				pageLength = options.get("pageLength").asLong();
			}
		}

		DatabaseClient client = getFinalClient();

		QueryManager mgr = client.newQueryManager();
		mgr.setPageLength(pageLength);

		StructuredQueryDefinition query = searchService.buildQueryWithCriteria(mgr.newStructuredQueryBuilder(type), searchRequest.get("filters"));
		return searchService.processResults(mgr.search(query, new JacksonHandle(), start).get());
	}
}
