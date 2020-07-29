package com.marklogic.envision.flows;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.Flows;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.impl.HubConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/flows")
public class FlowsController extends AbstractController {

	@Autowired
	FlowsController(HubConfigImpl hubConfig) {
		super(hubConfig);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	JsonNode getFlows(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		DatabaseClient client = getFinalClient();
		return Flows.on(client).getFlows();
	}
}
