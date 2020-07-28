package com.marklogic.envision.explore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.envision.dataServices.EntitySearcher;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.grove.boot.search.SearchService;
import com.marklogic.hub.impl.HubConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@RestController
@RequestMapping("/api/explore")
public class ExploreController extends AbstractController {

	final private ObjectMapper om = new ObjectMapper();
	final private SearchService searchService;

	@Autowired
	ExploreController(HubConfigImpl hubConfig, SearchService searchService) {
		super(hubConfig);
		this.searchService = searchService;
	}

    @RequestMapping(value = "/entities", method = RequestMethod.POST)
    JsonNode getEntities(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {

        JsonNode node = om.readTree(request.getInputStream());
        String qtext = "";
        if (node.has("qtext") && !node.get("qtext").isNull()) {
            qtext = node.get("qtext").asText();
        }
        int page = 1;
		if (node.has("page") && !node.get("page").isNull()) {
			page = node.get("page").asInt();
		}
		int pageLength = 10;
		if (node.has("pageLength") && !node.get("pageLength").isNull()) {
			pageLength = node.get("pageLength").asInt();
		}

		JsonNode filters = null;
		if (node.has("filters") && !node.get("filters").isNull()) {
			filters = node.get("filters");
		}

		String sort = "default";
		if (node.has("sort") && !node.get("sort").isNull()) {
			sort = node.get("sort").asText();
		}

		String database = "final";
		if (node.has("database") && !node.get("database").isNull()) {
			database = node.get("database").asText();
		}
		DatabaseClient client;
		if (database.equals("final")) {
			client = getFinalClient();
		}
		else {
			client = getStagingClient();
		}

		QueryManager mgr = client.newQueryManager();
		mgr.setPageLength(pageLength);
		StructuredQueryDefinition query = searchService.buildQueryWithCriteria(mgr.newStructuredQueryBuilder(), filters);

		if (database.equals("final")) {
			String filterString = ((StructuredQueryDefinition) query).serialize();
			JsonNode resp = EntitySearcher.on(client).findEntities(qtext, page, pageLength, sort, filterString);
			return resp;
		}
		else {
			long start = ((page - 1) * pageLength) + 1;
			return searchService.processResults(mgr.search(query, new JacksonHandle(), start).get());
		}
    }

	@RequestMapping(value = "/values", method = RequestMethod.POST)
	JsonNode getValus(HttpServletRequest request) throws IOException {

		JsonNode node = om.readTree(request.getInputStream());
		String facetName = node.get("facetName").asText();

		String qtext = "";
		if (node.has("qtext") && !node.get("qtext").isNull()) {
			qtext = node.get("qtext").asText();
		}

		JsonNode filters = null;
		if (node.has("filters") && !node.get("filters").isNull()) {
			filters = node.get("filters");
		}

		String database = "final";
		if (node.has("database") && !node.get("database").isNull()) {
			database = node.get("database").asText();
		}
		DatabaseClient client;
		if (database.equals("final")) {
			client = getFinalClient();
		}
		else {
			client = getStagingClient();
		}

		QueryManager mgr = client.newQueryManager();
		StructuredQueryDefinition query = searchService.buildQueryWithCriteria(mgr.newStructuredQueryBuilder(), filters);

		String filterString = ((StructuredQueryDefinition) query).serialize();
		JsonNode resp = EntitySearcher.on(client).getValues(facetName, qtext, filterString);
		return resp;
	}

    @RequestMapping(value = "/related-entities", method = RequestMethod.POST)
    JsonNode getRelatedEntities(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
        DatabaseClient client = getFinalClient();

        JsonNode node = om.readTree(request.getInputStream());
        String uri = node.get("uri").asText();
        String label = node.get("label").asText();
		int page = node.get("page").asInt();
		int pageLength = node.get("pageLength").asInt();
        return EntitySearcher.on(client).relatedEntities(uri, label, page, pageLength);
    }
}
