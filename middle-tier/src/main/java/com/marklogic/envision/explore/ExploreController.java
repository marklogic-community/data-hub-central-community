package com.marklogic.envision.explore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.envision.dataServices.EntitySearcher;
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

    @Autowired
    private HubConfigImpl hubConfig;

    ObjectMapper om = new ObjectMapper();

    @RequestMapping(value = "/entities", method = RequestMethod.POST)
    JsonNode getEntities(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
        DatabaseClient client = getFinalClient();

        JsonNode node = om.readTree(request.getInputStream());
        String qtext = null;
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

		String sort = "default";
		if (node.has("sort") && !node.get("sort").isNull()) {
			sort = node.get("sort").asText();
		}
        JsonNode entities = node.get("entities");
        JsonNode resp = EntitySearcher.on(client).findEntities(qtext, page, pageLength, sort, entities);
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
