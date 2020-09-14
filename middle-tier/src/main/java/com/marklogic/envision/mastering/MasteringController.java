package com.marklogic.envision.mastering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ResourceNotFoundException;
import com.marklogic.envision.dataServices.Mastering;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.grove.boot.error.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("/api/mastering")
public class MasteringController extends AbstractController {

	final private MasteringService masteringService;

	@Autowired
	MasteringController(MasteringService masteringService) {
		this.masteringService = masteringService;
	}

	private final ObjectMapper om = new ObjectMapper();

	@RequestMapping(value = "/doc", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<String> getDoc(@RequestParam String docUri) {
		HttpHeaders headers = new HttpHeaders();
		try {
			String body = masteringService.getDoc(getHubClient().getFinalClient(), docUri);
			if (body.startsWith("<")) {
				headers.setContentType(MediaType.APPLICATION_XML);
			} else {
				headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
			}
			return new ResponseEntity<>(body, headers, HttpStatus.OK);
		}
		catch(ResourceNotFoundException e) {
			throw new NotFoundException();
		}
	}

	@RequestMapping(value = "/history", method = RequestMethod.POST)
	public JsonNode getHistory(HttpServletRequest request) throws IOException {
		DatabaseClient client = getHubClient().getFinalClient();

		JsonNode node = om.readTree(request.getInputStream());
		String uri = node.get("uri").asText();
		return Mastering.on(client).getHistory(uri);
	}

	@RequestMapping(value = "/notification", method = RequestMethod.GET)
	public JsonNode getNotification(@RequestParam String uri) {
		JsonNode notification = masteringService.getNotification(getHubClient().getFinalClient(), uri);
		if (notification == null) {
			throw new NotFoundException();
		}
		return notification;
	}

	@RequestMapping(value = "/notifications", method = RequestMethod.POST)
	public JsonNode getNotifications(HttpServletRequest request) throws IOException {
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

		return masteringService.getNotifications(getHubClient().getFinalClient(), qtext, page, pageLength, sort);
	}

	@RequestMapping(value = "/blocks", method = RequestMethod.POST)
	public JsonNode getBlocks(@RequestBody ArrayNode uris) {
		return  masteringService.getBlocks(getHubClient().getFinalClient(), uris);
	}

	@RequestMapping(value = "/block", method = RequestMethod.POST)
	public JsonNode setBlocks(@RequestBody ArrayNode uris) {
		return  masteringService.block(getHubClient().getFinalClient(), uris);
	}

	@RequestMapping(value = "/unblock", method = RequestMethod.POST)
	public JsonNode unsetBlocks(@RequestBody ArrayNode uris) {
		return  masteringService.unblock(getHubClient().getFinalClient(), uris);
	}

	@RequestMapping(value = "/notifications", method = RequestMethod.PUT)
	public JsonNode updateNotifications(HttpServletRequest request) throws IOException {
		JsonNode node = om.readTree(request.getInputStream());
		ArrayNode uris = (ArrayNode)node.get("uris");

		String status = "";

		if (node.has("status") && !node.get("status").isNull()) {
			status = node.get("status").asText();
		}
		return masteringService.updateNotifications(getHubClient().getFinalClient(), uris, status);
	}

	@RequestMapping(value = "/notifications/delete", method = RequestMethod.POST)
	public ResponseEntity<?> deleteNotification(HttpServletRequest request) throws IOException {

		String[] uris = om.readValue(request.getInputStream(), String[].class);
		getHubClient().getFinalClient().newDocumentManager().delete(uris);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/merge", method = RequestMethod.POST)
	public JsonNode merge(HttpServletRequest request) throws IOException {
		JsonNode node = om.readTree(request.getInputStream());
		ArrayNode uris = (ArrayNode)node.get("uris");
		String flowName = null;
		if (node.has("flowName") && !node.get("flowName").isNull()) {
			flowName = node.get("flowName").asText();
		}
		String stepNumber = null;
		if (node.has("stepNumber") && !node.get("stepNumber").isNull()) {
			stepNumber = node.get("stepNumber").asText();
		}
		boolean preview = false;
		if (node.has("preview") && !node.get("preview").isNull()) {
			preview = node.get("preview").asBoolean();
		}
		return masteringService.mergeDocs(getHubClient().getFinalClient(), uris, flowName, stepNumber, preview);
	}

	@RequestMapping(value = "/unmerge", method = RequestMethod.POST)
	public JsonNode unmerge(HttpServletRequest request) throws IOException {
		JsonNode node = om.readTree(request.getInputStream());
		String uri = node.get("uri").asText();
		return masteringService.unmerge(getHubClient().getFinalClient(), uri);
	}
}
