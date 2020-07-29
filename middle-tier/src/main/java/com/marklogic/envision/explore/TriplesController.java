package com.marklogic.envision.explore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.Triples;
import com.marklogic.grove.boot.AbstractController;
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
@RequestMapping("/api/triples")
public class TriplesController extends AbstractController {

	private final ObjectMapper om = new ObjectMapper();

	@Autowired
	TriplesController(HubConfigImpl hubConfig) {
		super(hubConfig);
	}

	@RequestMapping(value = "/browse", method = RequestMethod.POST)
	JsonNode getTriples(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		JsonNode node = om.readTree(request.getInputStream());
		String qtext = null;
		if (!node.get("qtext").isNull()) {
			qtext = node.get("qtext").asText();
		}
		int page = node.get("page").asInt();
		int subjectsPerPage = node.get("subjectsPerPage").asInt();
		int linksPerSubject = node.get("linksPerSubject").asInt();
		String sort = node.get("sort").asText();
		String database = node.get("database").asText();
		DatabaseClient client = getProperClient(database);
		return Triples.on(client).browseTriples(qtext, page, subjectsPerPage, linksPerSubject, sort);
	}

	private DatabaseClient getProperClient(String database) {
		DatabaseClient client = null;
		switch (database) {
			case "staging":
				client = getStagingClient();
				break;
			case "final":
				client = getFinalClient();
				break;
			case "job":
				client = getJobClient();
				break;
		}
		return client;
	}

	@RequestMapping(value = "/related", method = RequestMethod.POST)
	JsonNode getRelated(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		JsonNode node = om.readTree(request.getInputStream());
		String item = node.get("item").asText();
		String itemId = node.get("itemId").asText();
		boolean isIRI = node.get("isIRI").asBoolean();
		String database = node.get("database").asText();
		String qtext = null;
		if (!node.get("qtext").isNull()) {
			qtext = node.get("qtext").asText();
		}
		String predicate = null;
		if (!node.get("predicate").isNull()) {
			qtext = node.get("predicate").asText();
		}
		int maxRelated = node.get("maxRelated").asInt();

		DatabaseClient client = getProperClient(database);
		return Triples.on(client).getRelated(item, itemId, isIRI, qtext, predicate, maxRelated);
	}
}
