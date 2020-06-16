package com.marklogic.grove.boot.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.io.StringHandle;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.grove.boot.error.NotFoundException;
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

    @Autowired
    ModelService modelService;

    @RequestMapping(value = "/{modelName}", method = RequestMethod.GET)
    public String getModel(HttpSession session, @PathVariable String modelName) {
        DatabaseClient client = getFinalClient();
        DocumentPage page = client.newDocumentManager().read(modelName);

        if (!page.hasNext()) {
            throw new NotFoundException();
        }
        DocumentRecord documentRecord = page.next();
        return documentRecord.getContent(new StringHandle()).get();
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public List<JsonNode> getAllModels() throws IOException {
		DatabaseClient client = getFinalClient();
    	return modelService.getAllModels(client);
    }

	@RequestMapping(value = "/delete", method = RequestMethod.POST)
    public void switchModeL(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		modelService.deleteModel(getFinalClient(), request.getInputStream());
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}

	@RequestMapping(value = "/rename", method = RequestMethod.POST)
	public void renameModeL(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		modelService.renameModel(getFinalClient(), request.getInputStream());
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
}
