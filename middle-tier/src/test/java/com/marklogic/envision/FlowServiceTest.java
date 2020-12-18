package com.marklogic.envision;

import com.marklogic.envision.flows.FlowsService;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import com.marklogic.hub.flow.RunFlowResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowServiceTest extends BaseTest {
	@Autowired
	ModelService modelService;

	@Autowired
	FlowsService flowsService;

	@BeforeEach
	void setUp() throws IOException {
		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();

		installHubModules();
		installEnvisionModules();

		registerAccount();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/MyHubModel.json"));
		flowsService.addMapping(hubClient, readJsonObject(getResource("mappings/myMappingStep.json")));
		flowsService.createFlow(hubClient, readJsonObject(getResourceFile("flows/runnable.flow.json")));

		installDoc(hubClient.getStagingClient(), "data/stagingDoc.json", "/doc1.json", "myFile.csv");
	}

	@Test
	void testRunSteps() throws IOException {
		flowsService.runSteps(getNonAdminHubClient(), "RunnableFlow", objectMapper.readTree("[\"1\"]"));
		assertEquals(1, getDocCount(getFinalClient(), "Person"));
	}
}
