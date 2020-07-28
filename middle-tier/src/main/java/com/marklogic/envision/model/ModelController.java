package com.marklogic.envision.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.impl.HubConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/models")
public class ModelController extends AbstractController {

    final private ModelService modelService;

    @Autowired
	ModelController(HubConfigImpl hubConfig, ModelService modelService) {
    	super(hubConfig);
    	this.modelService = modelService;
	}

    @RequestMapping(value = "/{modelName}", method = RequestMethod.GET)
    public JsonNode getModel(@PathVariable String modelName) {
        DatabaseClient client = getFinalClient();
        return modelService.getModel(client, modelName);
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public List<JsonNode> getAllModels() throws IOException {
		DatabaseClient client = getFinalClient();
    	return modelService.getAllModels(client);
    }

	@RequestMapping(value = "/delete", method = RequestMethod.POST)
    public void switchModel(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		modelService.deleteModel(getFinalClient(), request.getInputStream());
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}

    @RequestMapping(value = "/", method = RequestMethod.PUT)
    public void saveModel(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
        modelService.saveModel(getFinalClient(), request.getInputStream());
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

	@RequestMapping(value = "/import", method = RequestMethod.PUT)
	public void importModel(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		modelService.importModel(getFinalClient());
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}

    @RequestMapping(value = "/todatahub", method = RequestMethod.POST)
    public void toDataHub(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
        modelService.toDataHub(getFinalClient());
    }

	@RequestMapping(value = "/activeIndexes", method = RequestMethod.GET)
	public JsonNode getActiveIndexes(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		return modelService.getActiveIndexes(getFinalClient());
    }
    @RequestMapping(value = "/rename", method = RequestMethod.POST)
	public void renameModel(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		modelService.renameModel(getFinalClient(), request.getInputStream());
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}
}
