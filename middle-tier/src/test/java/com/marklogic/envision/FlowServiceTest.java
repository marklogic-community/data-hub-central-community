package com.marklogic.envision;

import com.marklogic.envision.flows.FlowsService;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.step.StepDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class FlowServiceTest extends BaseTest {
	@Autowired
	ModelService modelService;

	@Autowired
	FlowsService flowsService;

	@BeforeEach
	void setUp() throws IOException {
		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_MODULES_DB_NAME);

		installHubModules();
		installEnvisionModules();

		registerAccount();

		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/MyHubModel.json"));
		flowsService.createStep(hubClient, flowsService.createFlowFromJSON(readJsonObject(getResourceFile("flows/runnable.flow.json"))),readJsonObject(getResource("mappings/myMappingStep.json")));

		installDoc(hubClient.getStagingClient(), "data/stagingDoc.json", "/doc1.json", "myFile.csv");
	}

	@Test
	void testRunSteps() throws IOException {
		flowsService.runSteps(getNonAdminHubClient(), "RunnableFlow", objectMapper.readTree("[\"1\"]"));
		assertEquals(1, getDocCount(getFinalClient(), "Person"));
	}

	@Test
	void testCreateAndDeleteCustomStep() throws Exception {
		assertEquals(0, getDocCount(getStagingClient(), "http://marklogic.com/envision/" + ACCOUNT_NAME + "_step-definition"));
		assertEquals(0, getDocCount(getFinalClient(), "http://marklogic.com/envision/" + ACCOUNT_NAME + "_step-definition"));
		assertEquals(null, getModulesFile("/custom-modules/custom/DenormalizeEmployees/main.sjs"));
		Path dir = getHubConfig().getStepDefinitionPath(StepDefinition.StepDefinitionType.CUSTOM).resolve("DenormalizeEmployees");
		String stepFileName ="DenormalizeEmployees.step.json";
		File file = Paths.get(dir.toString(), stepFileName).toFile();
		assertFalse(file.exists());
		File mainFile = getHubConfig().getModulesDir().resolve("root/custom-modules/custom/DenormalizeEmployees/main.sjs").toFile();
		assertFalse(mainFile.exists());

		flowsService.createStep(getNonAdminHubClient(), "RunnableFlow",readJsonObject(getResource("steps/customStep.json")));
		assertTrue(file.exists());
		assertTrue(mainFile.exists());
		assertEquals(1, getDocCount(getStagingClient(), "http://marklogic.com/envision/" + ACCOUNT_NAME + "_step-definition"));
		assertEquals(1, getDocCount(getFinalClient(), "http://marklogic.com/envision/" + ACCOUNT_NAME + "_step-definition"));
		jsonAssertEquals(getResource("steps/customStepDefinition.json"),getDocumentString(getStagingClient(),"/step-definitions/custom/DenormalizeEmployees/DenormalizeEmployees.step.json"));
		jsonAssertEquals(getResource("steps/customStepDefinition.json"),getDocumentString(getFinalClient(),"/step-definitions/custom/DenormalizeEmployees/DenormalizeEmployees.step.json"));

		assertNotEquals("", getModulesFile("/custom-modules/custom/DenormalizeEmployees/main.sjs"));

		flowsService.deleteStep(getNonAdminHubClient(),"RunnableFlow", "DenormalizeEmployees");
		assertEquals(0, getDocCount(getStagingClient(), "http://marklogic.com/envision/" + ACCOUNT_NAME + "_step-definition"));
		assertEquals(0, getDocCount(getFinalClient(), "http://marklogic.com/envision/" + ACCOUNT_NAME + "_step-definition"));
		assertEquals(null, getModulesFile("/custom-modules/custom/DenormalizeEmployees/main.sjs"));
		assertFalse(file.exists());
		assertFalse(mainFile.exists());
	}

}
