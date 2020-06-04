package com.marklogic.envision.mastering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.io.StringHandle;
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
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

@RestController
@RequestMapping("/api/mastering")
public class MasteringController extends AbstractController {

	@Autowired
	private MasteringService masteringService;

	private ObjectMapper om = new ObjectMapper();

	@RequestMapping(value = "/doc", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<String> getDoc(@RequestParam String docUri) {
		HttpHeaders headers = new HttpHeaders();
		String body = masteringService.getDoc(getFinalClient(), docUri);
		if (body.startsWith("<")) {
			headers.setContentType(MediaType.APPLICATION_XML);
		} else {
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		}
		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}

	@RequestMapping(value = "/history", method = RequestMethod.POST)
	public JsonNode getEntities(HttpServletRequest request) throws IOException {
		DatabaseClient client = getFinalClient();

		JsonNode node = om.readTree(request.getInputStream());
		String uri = node.get("uri").asText();
		return Mastering.on(client).getHistory(uri);
	}

	@RequestMapping(value = "/notification", method = RequestMethod.GET)
	public JsonNode getNotification(@RequestParam String uri,HttpServletResponse response) throws IOException {
		return masteringService.getNotification(getFinalClient(), uri);
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

		JsonNode extractions = node.get("extractions");
		return masteringService.getNotifications(getFinalClient(), qtext, page, pageLength, sort);
	}

	@RequestMapping(value = "/blocks", method = RequestMethod.POST)
	public JsonNode getBlocks(@RequestBody ArrayNode uris) {
		return  masteringService.getBlocks(getFinalClient(), uris);
	}

	@RequestMapping(value = "/block", method = RequestMethod.POST)
	public JsonNode setBlocks(@RequestBody ArrayNode uris) {
		return  masteringService.setBlocks(getFinalClient(), uris);
	}

	@RequestMapping(value = "/unblock", method = RequestMethod.POST)
	public JsonNode unsetBlocks(@RequestBody ArrayNode uris) {
		return  masteringService.unsetBlocks(getFinalClient(), uris);
	}

	@RequestMapping(value = "/notifications", method = RequestMethod.PUT)
	public JsonNode updateNotifications(HttpServletRequest request) throws IOException {
		JsonNode node = om.readTree(request.getInputStream());
		ArrayNode uris = (ArrayNode)node.get("uris");

		String readStatus = null;
		String mergeStatus = null;
		String blockStatus = null;

		if (node.has("readStatus") && !node.get("readStatus").isNull()) {
			readStatus = node.get("readStatus").asText();
		}
		if (node.has("mergeStatus") && !node.get("mergeStatus").isNull()) {
			mergeStatus = node.get("mergeStatus").asText();
		}
		if (node.has("blockStatus") && !node.get("blockStatus").isNull()) {
			blockStatus = node.get("blockStatus").asText();
		}
		return masteringService.updateNotifications(getFinalClient(), uris, readStatus, mergeStatus, blockStatus);
	}

	@RequestMapping(value = "/notifications", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteNotification(HttpServletRequest request) throws IOException {

		String[] uris = om.readValue(request.getInputStream(), String[].class);
//		JsonNode node = om.readTree(request.getInputStream());
//		ArrayNode urisArray = (ArrayNode)node.get("uris");
//		ArrayList<String> uris = new ArrayList<>();
//		for (JsonNode jsonNode : urisArray) {
//			uris.add(jsonNode.asText());
//		}
//		getFinalClient().newDocumentManager().delete(uris.toArray(new String[0]));
		getFinalClient().newDocumentManager().delete(uris);
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
		return masteringService.mergeDocs(getFinalClient(), uris, flowName, stepNumber, preview);
	}

	@RequestMapping(value = "/unmerge", method = RequestMethod.POST)
	public JsonNode unmerge(HttpServletRequest request) throws IOException {
		JsonNode node = om.readTree(request.getInputStream());
		String uri = node.get("uri").asText();
		return masteringService.unmerge(getFinalClient(), uri);
	}
}
