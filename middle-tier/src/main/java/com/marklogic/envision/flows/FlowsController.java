package com.marklogic.envision.flows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.FailedRequestException;
import com.marklogic.envision.dataServices.Flows;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.dataservices.MappingService;
import com.marklogic.hub.dataservices.StepService;
import com.marklogic.hub.step.StepDefinition;
import org.apache.commons.collections4.IteratorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

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
		Iterator<JsonNode> stepsIterator = flowJson.path("steps").elements();
		List<JsonNode> stepsList = IteratorUtils.toList(stepsIterator);
		JsonNode[] steps = new JsonNode[stepsList.size()];
		flowsService.createStep(getHubClient(), flowsService.createFlowFromJSON(flowJson), stepsList.toArray(steps));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> deleteFlow(@RequestParam String flowId) {
		flowsService.deleteFlow(getHubClient(), flowId);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/customSteps/{stepName}", method = RequestMethod.GET)
	@ResponseBody
	public StepDefinition getCustomStep(@PathVariable String stepName) {
		return flowsService.getCustomStep(stepName);
	}

	@RequestMapping(value = "/mappings", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> addMapping(@RequestBody JsonNode mapping) throws IOException {
		if (!mapping.has("selectedSource")) {
			((ObjectNode) mapping).put("selectedSource", "query");
		}
		StepService.on(getHubClient().getStagingClient()).saveStep(StepDefinition.StepDefinitionType.MAPPING.toString(), mapping, false, false);
		try {
			flowsService.saveStepToProject(StepDefinition.StepDefinitionType.MAPPING.toString(), mapping);
		} catch (IOException e) {
			throw new RuntimeException("Failed to save mapping to project", e);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

    @RequestMapping(value = "/mappings/{mapName}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<JsonNode> getMapping(@PathVariable String mapName) {
    	try {
    		return new ResponseEntity<JsonNode>(StepService.on(getHubClient().getStagingClient()).getStep(StepDefinition.StepDefinitionType.MAPPING.toString(), mapName), HttpStatus.OK);
		} catch (FailedRequestException e) {
			return new ResponseEntity<JsonNode>(HttpStatus.NOT_FOUND);
		}
    }

	@RequestMapping(value = "/mappings/validate", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> validateMapping(@RequestBody JsonNode mapping, @RequestParam(value = "uri")String uri) {
		MappingService mappingService = MappingService.on(getHubClient().getStagingClient());
		return new ResponseEntity<>(mappingService.testMapping(uri, getHubClient().getDbName(DatabaseKind.STAGING), mapping), HttpStatus.OK);
	}

	@RequestMapping(value = "/mappings/functions", method = RequestMethod.GET)
	@ResponseBody
	public JsonNode getMappingFunctions() {
		MappingService mappingService = MappingService.on(getHubClient().getStagingClient());
		return mappingService.getMappingFunctions();
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
		String format = body.path("format").asText("json");
		String uri = body.path("uri").asText("");

		String response = Flows.on(getHubClient().getStagingClient()).previewMapping(mappingName, format, uri);
		HttpHeaders headers = new HttpHeaders();
		if (format.equals("json")) {
			headers.setContentType(MediaType.APPLICATION_JSON);
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
		flowsService.runStepsAsync(getHubClient(), flowName, steps);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
