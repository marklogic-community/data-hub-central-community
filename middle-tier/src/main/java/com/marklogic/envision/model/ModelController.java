package com.marklogic.envision.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.grove.boot.error.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/models")
public class ModelController extends AbstractController {

    final private ModelService modelService;

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
    	return modelService.getAllModels(getHubClient());
    }

	@RequestMapping(value = "/delete", method = RequestMethod.POST)
    public void switchModel(HttpServletRequest request, HttpServletResponse response) throws IOException {
		modelService.deleteModel(getHubClient().getUsername(), request.getInputStream());
		response.setStatus(HttpStatus.NO_CONTENT.value());
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
