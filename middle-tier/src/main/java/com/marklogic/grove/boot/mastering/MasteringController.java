package com.marklogic.grove.boot.mastering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.envision.Mastering;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@RestController
@RequestMapping("/api/mastering")
public class MasteringController extends AbstractController {

	ObjectMapper om = new ObjectMapper();

	@RequestMapping(value = "/unmerge", method = RequestMethod.POST)
	JsonNode getEntities(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		DatabaseClient client = getFinalClient();

		JsonNode node = om.readTree(request.getInputStream());
		String uri = node.get("uri").asText();
		return Mastering.on(client).unmerge(uri);
	}
}
