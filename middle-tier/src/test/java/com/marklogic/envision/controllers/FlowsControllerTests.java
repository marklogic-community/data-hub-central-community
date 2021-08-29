package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.FailedRequestException;
import com.marklogic.envision.flows.FlowsService;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import com.marklogic.hub.FlowManager;
import com.marklogic.hub.MappingManager;
import com.marklogic.hub.dataservices.StepService;
import com.marklogic.hub.error.DataHubProjectException;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.step.StepDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FlowsControllerTests extends AbstractMvcTest {
	private static final String GET_FLOWS_URL = "/api/flows/";
	private static final String VALIDATE_MAPPING_URL = "/api/flows/mappings/validate";
	private static final String GET_MAPPING_FUNCTIONS_URL = "/api/flows/mappings/functions";
	private static final String GET_SAMPLE_DOC_URL = "/api/flows/mappings/sampleDoc";
	private static final String MAPPINGS_PREVIEW_URL = "/api/flows/mappings/preview";
	private static final String NEW_STEP_INFO_URL = "/api/flows/newStepInfo";
	private static final String CREATE_STEP_URL = "/api/flows/steps";
	private static final String DELETE_STEP_URL = "/api/flows/steps/delete";
	private static final String RUN_STEP_URL = "/api/flows/steps/run";

	@Autowired
	FlowsService flowsService;

	@Autowired
	FlowManager flowManager;

	@Autowired
	MappingManager mappingManager;

	@Autowired
	ModelService modelService;

	@BeforeEach
	public void setup() throws IOException {
		envisionConfig.setMultiTenant(true);
		super.setup();
		try {
			removeUser(ACCOUNT_NAME);
		} catch (FailedRequestException e) {
			// User is already removed
		}
		clearStagingFinalAndJobDatabases();

		registerAccount();
	}

	@Test
	void getFlows() throws Exception {
		getJson(GET_FLOWS_URL)
			.andExpect(status().isUnauthorized());

		login();
		getJson(GET_FLOWS_URL)
			.andDo(
				result -> {
					ArrayNode node = readJsonArray(result.getResponse().getContentAsString());
					assertEquals(0, node.size());
				})
			.andExpect(status().isOk());
	}

	@Test
	void getFlow() throws Exception {
		getJson(GET_FLOWS_URL + "bogus")
			.andExpect(status().isUnauthorized());

		login();

		// doesn't exist
		getJson(GET_FLOWS_URL + "bogus")
			.andDo(
				result -> {
					JsonNode node = readJsonObject(result.getResponse().getContentAsString());
					JSONAssert.assertEquals(getResource("models/bogusModel.json"), objectMapper.writeValueAsString(node), true);
				})
			.andExpect(status().isOk());

		// already exists
		getJson(GET_FLOWS_URL + "bogus")
			.andDo(
				result -> {
					JsonNode node = readJsonObject(result.getResponse().getContentAsString());
					JSONAssert.assertEquals(getResource("models/bogusModel.json"), objectMapper.writeValueAsString(node), true);
				})
			.andExpect(status().isOk());
	}

	@Test
	void updateFlow() throws Exception {
		putJson("/api/flows/MyFlow", "{}")
			.andExpect(status().isUnauthorized());

		login();

		ArrayNode flows = (ArrayNode)flowsService.getFlows(getNonAdminHubClient().getFinalClient());
		assertEquals(0, flows.size());
		Flow newFlow = flowManager.createFlow("MyFlow");
		putJson("/api/flows/MyFlow", objectMapper.valueToTree(newFlow))
			.andExpect(status().isOk());

		flows = (ArrayNode)flowsService.getFlows(getNonAdminHubClient().getFinalClient());
		assertEquals(1, flows.size());
		assertFlowEquals(newFlow, flowsService.createFlowFromJSON(flows.get(0)));
	}

	@Test
	void getMapping() throws Exception {
		getJson("/api/flows/mappings/bogus")
			.andExpect(status().isUnauthorized());

		login();

		getJson("/api/flows/mappings/bogus")
			.andExpect(status().isNotFound());
	}

	@Test
	void addMapping() throws Exception {
		File stepPathDir = envisionConfig.dhfDir.toPath().toAbsolutePath().resolve("steps").resolve("mapping").toAbsolutePath().toFile();
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/model.json"));

		postJson("/api/flows/mappings/", "{\"lang\":\"zxx\",\"name\":\"wacky\",\"description\":\"Default description\",\"version\":1,\"targetEntityType\":\"http://example.org/modelName-version/entityType\",\"sourceContext\":\"/\",\"sourceURI\":\"\",\"properties\":{},\"namespaces\":{}}")
			.andExpect(status().isUnauthorized());

		login();

		File stepFile = stepPathDir.toPath().resolve( "wacky.step.json").toFile();
		assertFalse(stepFile.exists(), "Mapping step shouldn't be written yet.");
		assertThrows(DataHubProjectException.class,() -> mappingManager.getMappingAsJSON("wacky", -1, false));

		postJson("/api/flows/mappings/", "{\"lang\":\"zxx\",\"name\":\"wacky\",\"description\":\"Default description\",\"version\":1,\"targetEntityType\":\"http://marklogic.com/envision/Planet-0.0.1/Planet\",\"sourceContext\":\"/\",\"sourceURI\":\"\",\"properties\":{},\"namespaces\":{}}")
			.andExpect(status().isOk());

		assertDoesNotThrow(
			() -> {
				StepService.on(getStagingClient()).getStep(StepDefinition.StepDefinitionType.MAPPING.toString(), "wacky");
			}
		);
		assertTrue(stepFile.exists(), "Mapping step should be written now.");
	}

	@Test
	void validateMapping() throws Exception {
		installDoc(getNonAdminHubClient().getStagingClient(), "data/testFile.json", "/testFile.json");

		postJson(VALIDATE_MAPPING_URL +"?uri=/testFile.json","{\"lang\":\"zxx\",\"name\":\"wacky\",\"description\":\"Default description\",\"version\":1,\"targetEntityType\":\"http://marklogic.com/envision/Planet-0.0.1/Planet\",\"sourceContext\":\"/\",\"sourceURI\":\"\",\"properties\":{},\"namespaces\":{}}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(VALIDATE_MAPPING_URL +"?uri=/testFile.json","{\"lang\":\"zxx\",\"name\":\"wacky\",\"description\":\"Default description\",\"version\":1,\"targetEntityType\":\"http://marklogic.com/envision/Planet-0.0.1/Planet\",\"sourceContext\":\"/\",\"sourceURI\":\"\",\"properties\":{},\"namespaces\":{}}")
			.andDo(
				result -> {
					JsonNode node = readJsonObject(result.getResponse().getContentAsString());
					JSONAssert.assertEquals("{\"lang\":\"zxx\",\"name\":\"wacky\",\"description\":\"Default description\",\"version\":1,\"targetEntityType\":\"http://marklogic.com/envision/Planet-0.0.1/Planet\",\"sourceContext\":\"/\",\"sourceURI\":\"\",\"properties\":{},\"namespaces\":{},\"uriExpression\":{\"output\":\"/testFile.json\"},\"expressionContext\":\"/\"}", objectMapper.writeValueAsString(node), true);
				})
			.andExpect(status().isOk());
	}

	@Test
	void getMappingFunctions() throws Exception {
		getJson(GET_MAPPING_FUNCTIONS_URL)
			.andExpect(status().isUnauthorized());

		login();

		getJson(GET_MAPPING_FUNCTIONS_URL)
			.andDo(
				result -> {
					ArrayNode node = readJsonArray(result.getResponse().getContentAsString());
					assertTrue(node.size() > 100);
				})
			.andExpect(status().isOk());
	}

	@Test
	void getSampleDoc() throws Exception {
		installDoc(getNonAdminHubClient().getStagingClient(), "data/stagingDoc.json", "/doc1.json");
		postJson(GET_SAMPLE_DOC_URL, "{ \"uri\": \"/doc1.json\" }")
			.andExpect(status().isUnauthorized());

		login();

		postJson(GET_SAMPLE_DOC_URL, "{ \"uri\": \"/doc1.json\", \"namespaces\": {} }")
			.andDo(
				result -> {
					ArrayNode node = readJsonArray(result.getResponse().getContentAsString());
					JSONAssert.assertEquals("[{\"ns\":null,\"nsPrefix\":null,\"name\":\"name\",\"xpath\":\"name\",\"value\":\"sue\"},{\"ns\":null,\"nsPrefix\":null,\"name\":\"age\",\"xpath\":\"age\",\"value\":\"24\"}]", objectMapper.writeValueAsString(node), true);
				})
			.andExpect(status().isOk());
	}

	@Test
	void previewMapping() throws Exception {
//		postJson(MAPPINGS_PREVIEW_URL)
	}

	@Test
	void getNewStepInfo() throws Exception {
		getJson(NEW_STEP_INFO_URL)
			.andExpect(status().isUnauthorized());

		login();

		getJson(NEW_STEP_INFO_URL)
			.andDo(
				result -> {
					JsonNode node = readJsonObject(result.getResponse().getContentAsString());
					JSONAssert.assertEquals("{\"databases\":{\"staging\":\"data-hub-STAGING\",\"final\":\"data-hub-FINAL\"},\"collections\":{\"staging\":[],\"final\":[]}}", objectMapper.writeValueAsString(node), true);
				})
			.andExpect(status().isOk());
	}

	@Test
	void createStep() throws Exception {
		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/model.json"));
		flowsService.createFlow(hubClient, readJsonObject(getResourceFile("flows/testFlow.flow.json")));

		JsonNode flow = flowsService.getJsonFlow(hubClient, "MyCrazyFlow");
		assertFalse(flow.get("steps").has("1"));

		postJson(CREATE_STEP_URL, "{ \"flowName\": \"MyCrazyFlow\", \"step\": " + getResource("steps/MyMappingStep.json") + " }")
			.andExpect(status().isUnauthorized());

		login();

		postJson(CREATE_STEP_URL, "{ \"flowName\": \"MyCrazyFlow\", \"step\": " + getResource("steps/MyMappingStep.json") + " }")
			.andExpect(status().isOk());

		flow = flowsService.getJsonFlow(hubClient, "MyCrazyFlow");
		assertEquals(flow.path("steps").path("1").path("stepId").asText(), "myMappingStep-mapping");
		assertDoesNotThrow(
			() -> {
				StepService.on(getStagingClient()).getStep(StepDefinition.StepDefinitionType.MAPPING.toString(), "myMappingStep");
			}
		);
	}

	@Test
	void deleteStep() throws Exception {
		HubClient hubClient = getNonAdminHubClient();
		flowsService.createFlow(hubClient, readJsonObject(getResourceFile("flows/testFlowWithSteps.flow.json")));

		JsonNode flow = flowsService.getJsonFlow(hubClient, "MyCrazyFlow");
		assertEquals(3, flow.get("steps").size());
		assertEquals("myMappingStep1", flow.get("steps").get("1").get("name").asText());
		assertEquals("myMappingStep2", flow.get("steps").get("2").get("name").asText());
		assertEquals("myMappingStep3", flow.get("steps").get("3").get("name").asText());

		postJson(DELETE_STEP_URL, "{\"flowName\": \"MyCrazyFlow\", \"stepName\": \"myMappingStep2\"}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(DELETE_STEP_URL, "{\"flowName\": \"MyCrazyFlow\", \"stepName\": \"myMappingStep2\"}")
			.andExpect(status().isOk());

		flow = flowsService.getJsonFlow(hubClient, "MyCrazyFlow");
		assertEquals(2, flow.get("steps").size());
		assertEquals("myMappingStep1", flow.get("steps").get("1").get("name").asText());
		assertEquals("myMappingStep3", flow.get("steps").get("2").get("name").asText());


		postJson(DELETE_STEP_URL, "{\"flowName\": \"MyCrazyFlow\", \"stepName\": \"myMappingStep1\"}")
			.andExpect(status().isOk());

		flow = flowsService.getJsonFlow(hubClient, "MyCrazyFlow");
		assertEquals(1, flow.get("steps").size());
		assertEquals("myMappingStep3", flow.get("steps").get("1").get("name").asText());

		postJson(DELETE_STEP_URL, "{\"flowName\": \"MyCrazyFlow\", \"stepName\": \"myMappingStep3\"}")
			.andExpect(status().isOk());

		flow = flowsService.getJsonFlow(hubClient, "MyCrazyFlow");
		assertEquals(0, flow.get("steps").size());

	}

	@Test
	void runSteps() throws Exception {
		HubClient hubClient = getNonAdminHubClient();

		modelService.saveModel(hubClient, getResourceStream("models/MyHubModel.json"));
		flowsService.createStep(hubClient, flowsService.createFlowFromJSON(readJsonObject(getResourceFile("flows/runnable.flow.json"))),readJsonObject(getResource("mappings/myMappingStep.json")));

		installDoc(hubClient.getStagingClient(), "data/stagingDoc.json", "/doc1.json", "myFile.csv");

		postJson(RUN_STEP_URL, "{\"flowName\", \"RunnableFlow\", \"steps\": [\"1\"]}")
			.andExpect(status().isUnauthorized());

		login();

		postJson(RUN_STEP_URL, "{\"flowName\": \"RunnableFlow\", \"steps\": [\"1\"]}")
			.andExpect(status().isOk());
	}


}
