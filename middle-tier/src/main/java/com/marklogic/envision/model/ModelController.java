package com.marklogic.envision.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.grove.boot.error.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/models")
public class ModelController extends AbstractController {

    final private ModelService modelService;

    final private  ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
	ModelController(ModelService modelService) {
    	this.modelService = modelService;
	}

    @RequestMapping(value = "/current", method = RequestMethod.GET)
    public JsonNode getModel() {
        JsonNode model = modelService.getModel(getHubClient().getFinalClient());
        if (model != null) {
        	return model;
		}
		throw new NotFoundException();
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public List<JsonNode> getAllModels() throws IOException {
		HubClient hubClient = getHubClient();
    	return modelService.getAllModels(hubClient, hubClient.getUsername());
    }

	@RequestMapping(value = "/delete", method = RequestMethod.POST)
    public void switchModel(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JsonNode node = objectMapper.readTree(request.getInputStream());
		String modelName = node.get("name").asText();
		if (modelService.deleteModel(getHubClient(), getHubClient().getUsername(), modelName)) {
			response.setStatus(HttpStatus.NO_CONTENT.value());
		}
		else {
			response.setStatus(HttpStatus.NOT_FOUND.value());
		}
	}

    @RequestMapping(value = "/", method = RequestMethod.PUT)
    public void saveModel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        modelService.saveModel(getHubClient(), request.getInputStream());
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

	@RequestMapping(value = "/import", method = RequestMethod.PUT)
	public void importModel(HttpServletResponse response) throws IOException {
		modelService.importModel(getHubClient());
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}

	@RequestMapping(value = "/activeIndexes", method = RequestMethod.GET)
	public JsonNode getActiveIndexes() {
		return modelService.getActiveIndexes(getHubClient().getFinalClient());
    }

    @RequestMapping(value = "/rename", method = RequestMethod.POST)
	public void renameModel(HttpServletRequest request, HttpServletResponse response) throws IOException {
		modelService.renameModel(getHubClient().getUsername(), getHubClient(), request.getInputStream());
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}
}
