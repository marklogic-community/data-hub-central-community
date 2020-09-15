package com.marklogic.envision.flows;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.envision.dataServices.Flows;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.mapping.MappingFunctions;
import com.marklogic.hub.mapping.MappingValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/flows")
public class FlowsController extends AbstractController {

	final private FlowsService flowsService;

	@Autowired
	FlowsController(FlowsService flowsService) {
		this.flowsService = flowsService;
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	JsonNode getFlows() {
		return flowsService.getFlows(getHubClient().getFinalClient());
	}

	@RequestMapping(value = "/{flowName}", method = RequestMethod.GET)
	JsonNode getFlow(@PathVariable String flowName) {
		return flowsService.getJsonFlow(getHubClient(), flowName);
	}

	@RequestMapping(value = "/{flowName}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<?> updateFlow(@PathVariable String flowName, @RequestBody JsonNode flowJson) {
		flowsService.createFlow(getHubClient(), flowJson);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/mappings/{mapName}", method = RequestMethod.GET)
	@ResponseBody
	public String getMapping(@PathVariable String mapName) {
		return flowsService.getMapping(mapName);
	}

	@RequestMapping(value = "/mappings", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> addMapping(@RequestBody JsonNode mapping) throws IOException {
		flowsService.addMapping(getHubClient(), mapping);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/mappings/validate", method = RequestMethod.POST)
	@ResponseBody
	public JsonNode validateMapping(@RequestBody JsonNode mapping, @RequestParam(value = "uri")String uri) {
		MappingValidator mappingValidator = new MappingValidator(getHubClient().getStagingClient());
		return mappingValidator.validateJsonMapping(mapping.toString(), uri);
	}

	@RequestMapping(value = "/mappings/functions", method = RequestMethod.GET)
	@ResponseBody
	public JsonNode getMappingFunctions() {
		MappingFunctions mappingFunctions = new MappingFunctions(getHubClient().getStagingClient());
		return mappingFunctions.getMappingFunctions();
	}

	@RequestMapping(value = "/mappings/sampleDoc", method = RequestMethod.POST)
	@ResponseBody
	public JsonNode getSampleDoc(@RequestBody JsonNode body) {
		JsonNode namespaces = body.get("namespaces");
		String uri = body.get("uri").asText();
		return Flows.on(getHubClient().getStagingClient()).getSample(uri, namespaces);
	}

	@RequestMapping(value = "/mappings/preview", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> previewMapping(@RequestBody JsonNode body) {
		String mappingName = body.get("mappingName").asText();
		int mappingVersion = body.get("mappingVersion").asInt();
		String format = body.get("format").asText();
		String uri = body.get("uri").asText();

		String response = Flows.on(getHubClient().getStagingClient()).previewMapping(mappingName, mappingVersion, format, uri);
		HttpHeaders headers = new HttpHeaders();
		if (format.equals("json")) {
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		} else {
			headers.setContentType(MediaType.APPLICATION_XML);
		}
		return new ResponseEntity<>(response, headers, HttpStatus.OK);
	}

	@RequestMapping(value = "/newStepInfo", method = RequestMethod.GET)
	@ResponseBody
	public JsonNode getNewStepInfo() {
		return Flows.on(getHubClient().getStagingClient()).newStepInfo();
	}

	@RequestMapping(value = "/steps", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> createStep(@RequestBody JsonNode body) {
		String flowName = body.get("flowName").asText();
		JsonNode stepJson = body.get("step");
		flowsService.createStep(getHubClient(), flowName, stepJson);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/steps/delete", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> deleteStep(@RequestBody JsonNode body) {
		String flowName = body.get("flowName").asText();
		String stepName = body.get("stepName").asText();
		flowsService.deleteStep(getHubClient(), flowName, stepName);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/steps/run", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> runSteps(@RequestBody JsonNode body) {
		String flowName = body.get("flowName").asText();
		JsonNode steps = body.get("steps");
		flowsService.runSteps(getHubClient(), flowName, steps);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
