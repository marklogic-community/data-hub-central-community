package com.marklogic.envision.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.envision.auth.UpdatePasswordPojo;
import com.marklogic.envision.auth.UserPojo;
import com.marklogic.envision.flows.FlowsService;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import com.marklogic.hub.HubConfig;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.io.File;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthenticationTests extends AbstractMvcTest {
	private static final String LOGIN_URL = "/api/auth/login";
	private static final String SIGNUP_URL = "/api/auth/signup";
	private static final String USER_EXISTS_URL = "/api/auth/userExists";
	private static final String RESET_PASSWORD_URL = "/api/auth/resetPassword";
	private static final String REGISTRATION_COMPLETE_URL = "/registrationConfirm";
	private static final String VALIDATE_RESET_TOKEN_URL = "/api/auth/validateResetToken";
	private static final String UPDATE_PASSWORD_URL = "/api/auth/updatePassword";
	private static final String GET_PROFILE_URL = "/api/auth/profile";
	private static final String DELETE_USER_URL = "/api/auth/delete";

	@Autowired
	ModelService modelService;

	@Autowired
	FlowsService flowsService;
	@BeforeEach
	void setup() {
		removeUser(ACCOUNT_NAME);
		removeUser(ADMIN_ACCOUNT_NAME);
		removeUser(ACCOUNT_NAME2);
		envisionConfig.setMultiTenant(true);

		// remove models
		File modelDir = modelService.getModelsDir(true, ACCOUNT_NAME);
		for (File file: Objects.requireNonNull(modelDir.listFiles())) {
			file.delete();
		}
		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME, HubConfig.DEFAULT_STAGING_SCHEMAS_DB_NAME);
		installEnvisionModules();
	}

	@Test
	void loginWithInvalidCredentials() throws Exception {
		assertNull(authToken);
		String payload = buildLoginPayload("fake",ACCOUNT_PASSWORD);
		mockMvc
			.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(payload))
			.andExpect(status().isUnauthorized());
		assertNull(authToken);
	}

	@Test
	void loginWithValidCredentials() throws Exception {
		assertNull(authToken);
		loginAsUser("flow-developer", ACCOUNT_PASSWORD).andDo(
			result -> {
				String authToken = result.getResponse().getHeader("Authorization");
				assertNotNull(authToken);
				assertTrue(authToken.startsWith("Bearer "));
			})
			.andExpect(status().isOk());

		assertNotNull(authToken);
	}

	@Test
	void testRegisterAccount() throws Exception {
		UserPojo user = new UserPojo();
		user.email = ACCOUNT_NAME;
		user.password = ACCOUNT_PASSWORD;
		user.name = "Bob Smith";
		assertNull(user.token);
		assertNull(user.validated);

		postJson(SIGNUP_URL, user)
			.andExpect(status().isOk());
		verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString());
		Mockito.reset(emailService);

		UserPojo finalUser = userService.getUser(getFinalClient(), ACCOUNT_NAME);
		assertFalse(finalUser.token.isEmpty());
		assertFalse(finalUser.validated);

		getJson(REGISTRATION_COMPLETE_URL + "?token=bogus")
			.andExpect(status().isFound());

		getJson(REGISTRATION_COMPLETE_URL + "?token=" + finalUser.token)
			.andDo(
				result -> {
					assertEquals("http://localhost:-1/registrationComplete?accountVerified=true", result.getResponse().getHeader("Location"));
				})
			.andExpect(status().isFound());

		finalUser = userService.getUser(getFinalClient(), ACCOUNT_NAME);
		assertTrue(finalUser.validated);
		assertNull(finalUser.token);
		assertNull(authToken);

		File modelDir = modelService.getModelsDir(true, ACCOUNT_NAME);
		File modelFile = new File(modelDir, "MyModel.json");
		jsonAssertEquals("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}", FileUtils.readFileToString(modelFile));
	}

	@Test
	void testRegisterAccountExpiredToken() throws Exception {
		UserPojo user = new UserPojo();
		user.email = ACCOUNT_NAME;
		user.password = ACCOUNT_PASSWORD;
		user.name = "Bob Smith";
		assertNull(user.token);
		assertNull(user.validated);

		postJson(SIGNUP_URL, user)
			.andExpect(status().isOk());
		verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString());
		Mockito.reset(emailService);

		UserPojo finalUser = userService.getUser(getFinalClient(), ACCOUNT_NAME);
		assertFalse(finalUser.token.isEmpty());
		assertFalse(finalUser.validated);

		getJson(REGISTRATION_COMPLETE_URL + "?token=bogus")
			.andExpect(status().isFound());


		// expire the token
		final Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, -2);
		finalUser.tokenExpiry = c.getTime();
		userService.saveUser(finalUser);

		getJson(REGISTRATION_COMPLETE_URL + "?token=" + finalUser.token)
			.andDo(
				result -> {
					assertEquals("http://localhost:-1/registrationComplete?error=Expired+Token", result.getResponse().getHeader("Location"));
				})
			.andExpect(status().isFound());

		finalUser = userService.getUser(getFinalClient(), ACCOUNT_NAME);
		assertFalse(finalUser.token.isEmpty());
		assertFalse(finalUser.validated);
		assertNull(authToken);

		File modelDir = modelService.getModelsDir(true, ACCOUNT_NAME);
		File modelFile = new File(modelDir, "MyModel.json");
		assertTrue(modelFile.exists());
	}

	@Test
	void userExists() throws Exception {
		getJson(USER_EXISTS_URL + "?email=dont.exist@marklogic.com")
			.andDo(
				result -> assertFalse(objectMapper.readTree(result.getResponse().getContentAsString()).asBoolean()))
			.andExpect(status().isOk());

		registerAccount();
		getJson(USER_EXISTS_URL + "?email=bob.smith@marklogic.com")
			.andDo(
				result -> assertTrue(objectMapper.readTree(result.getResponse().getContentAsString()).asBoolean()))
			.andExpect(status().isOk());
		assertNull(authToken);
	}

	@Test
	void deleteUserMultiTenant() throws Exception {
		envisionConfig.setMultiTenant(true);
		registerAccount();
		registerAccount(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
		registerEnvisionAdminAccount();
		HubClient adminHubClient = getAdminHubClient();
		HubClient hubClient = getNonAdminHubClient();
		modelService.saveModel(hubClient, getResourceStream("models/MyHubModel.json"));
		flowsService.addMapping(hubClient, readJsonObject(getResource("mappings/myMappingStep.json")));
		flowsService.createFlow(hubClient, readJsonObject(getResourceFile("flows/user.flow.json")));
		installDoc(hubClient.getStagingClient(), "data/stagingDoc.json", "/ingest/" + ACCOUNT_NAME + "/doc1.json", "user-data");
		installDoc(hubClient.getFinalClient(), "data/stagingDoc.json", "/data/" + ACCOUNT_NAME + "/doc1.json", "user-data");

		hubClient = getHubClient(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
		modelService.saveModel(hubClient, getResourceStream("models/MyHubModel.json"));
		flowsService.addMapping(hubClient, readJsonObject(getResource("mappings/myMappingStep2.json")));
		flowsService.createFlow(hubClient, readJsonObject(getResourceFile("flows/user2.flow.json")));
		installDoc(hubClient.getStagingClient(), "data/stagingDoc.json", "/ingest/" + ACCOUNT_NAME2 + "/doc1.json", "user-data");
		installDoc(hubClient.getFinalClient(), "data/stagingDoc.json", "/data/" + ACCOUNT_NAME2 + "/doc1.json", "user-data");

		assertEquals(3, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/envision/usr"));
		assertEquals(2, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/data-hub/flow"));
		assertEquals(2, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/data-hub/mappings"));
		assertEquals(2, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(2, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/envision/model"));
		assertEquals(2, getDocCount(adminHubClient.getFinalClient(), "user-data"));

		assertEquals(2, getDocCount(adminHubClient.getStagingClient(), "http://marklogic.com/data-hub/flow"));
		assertEquals(2, getDocCount(adminHubClient.getStagingClient(), "http://marklogic.com/data-hub/mappings"));
		assertEquals(2, getDocCount(adminHubClient.getStagingClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(2, getDocCount(adminHubClient.getStagingClient(), "user-data"));

		assertEquals(2, getDocCount(adminHubClient.getFinalSchemasClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(2, getDocCount(adminHubClient.getFinalSchemasClient(), "ml-data-hub-json-schema"));
		assertEquals(2, getDocCount(adminHubClient.getFinalSchemasClient(), "ml-data-hub-xml-schema"));
		assertEquals(1, getDocCount(adminHubClient.getFinalSchemasClient(), ACCOUNT_NAME));

		assertEquals(2, getDocCount(adminHubClient.getStagingSchemasClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(2, getDocCount(adminHubClient.getStagingSchemasClient(), "ml-data-hub-json-schema"));
		assertEquals(2, getDocCount(adminHubClient.getStagingSchemasClient(), "ml-data-hub-xml-schema"));

		getJson(DELETE_USER_URL + "?username=" + ACCOUNT_NAME)
			.andExpect(status().isUnauthorized());

		login();

		getJson(DELETE_USER_URL + "?username=" + ACCOUNT_NAME)
			.andExpect(status().isForbidden());

		loginAsUser(ADMIN_ACCOUNT_NAME, ACCOUNT_PASSWORD);

		getJson(DELETE_USER_URL + "?username=" + ACCOUNT_NAME)
			.andExpect(status().isOk());

		assertEquals(2, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/envision/usr"));
		assertEquals(1, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/data-hub/flow"));
		assertEquals(1, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/data-hub/mappings"));
		assertEquals(1, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(1, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/envision/model"));
		assertEquals(1, getDocCount(adminHubClient.getFinalClient(), "user-data"));

		assertEquals(1, getDocCount(adminHubClient.getStagingClient(), "http://marklogic.com/data-hub/flow"));
		assertEquals(1, getDocCount(adminHubClient.getStagingClient(), "http://marklogic.com/data-hub/mappings"));
		assertEquals(1, getDocCount(adminHubClient.getStagingClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(1, getDocCount(adminHubClient.getStagingClient(), "user-data"));

		assertEquals(1, getDocCount(adminHubClient.getFinalSchemasClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(1, getDocCount(adminHubClient.getFinalSchemasClient(), "ml-data-hub-json-schema"));
		assertEquals(1, getDocCount(adminHubClient.getFinalSchemasClient(), "ml-data-hub-xml-schema"));
		assertEquals(0, getDocCount(adminHubClient.getFinalSchemasClient(), ACCOUNT_NAME));
		assertEquals(1, getDocCount(adminHubClient.getFinalSchemasClient(), ACCOUNT_NAME2));

		assertEquals(1, getDocCount(adminHubClient.getStagingSchemasClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(1, getDocCount(adminHubClient.getStagingSchemasClient(), "ml-data-hub-json-schema"));
		assertEquals(1, getDocCount(adminHubClient.getStagingSchemasClient(), "ml-data-hub-xml-schema"));

		assertNull(adminHubClient.getStagingClient().newDocumentManager().exists("/ingest/" + ACCOUNT_NAME + "/doc1.json"));
		assertNull(adminHubClient.getFinalClient().newDocumentManager().exists("/data/" + ACCOUNT_NAME + "/doc1.json"));
		assertNotNull(adminHubClient.getStagingClient().newDocumentManager().exists("/ingest/" + ACCOUNT_NAME2 + "/doc1.json"));
		assertNotNull(adminHubClient.getFinalClient().newDocumentManager().exists("/data/" + ACCOUNT_NAME2 + "/doc1.json"));

		loginAsUser(ADMIN_ACCOUNT_NAME, ACCOUNT_PASSWORD);

		getJson(DELETE_USER_URL + "?username=" + ACCOUNT_NAME2)
			.andExpect(status().isOk());

		assertEquals(1, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/envision/usr"));
		assertEquals(0, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/data-hub/flow"));
		assertEquals(0, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/data-hub/mappings"));
		assertEquals(0, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(0, getDocCount(adminHubClient.getFinalClient(), "http://marklogic.com/envision/model"));
		assertEquals(0, getDocCount(adminHubClient.getFinalClient(), "user-data"));

		assertEquals(0, getDocCount(adminHubClient.getStagingClient(), "http://marklogic.com/data-hub/flow"));
		assertEquals(0, getDocCount(adminHubClient.getStagingClient(), "http://marklogic.com/data-hub/mappings"));
		assertEquals(0, getDocCount(adminHubClient.getStagingClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(0, getDocCount(adminHubClient.getStagingClient(), "user-data"));

		assertEquals(0, getDocCount(adminHubClient.getFinalSchemasClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(0, getDocCount(adminHubClient.getFinalSchemasClient(), "ml-data-hub-json-schema"));
		assertEquals(0, getDocCount(adminHubClient.getFinalSchemasClient(), "ml-data-hub-xml-schema"));
		assertEquals(0, getDocCount(adminHubClient.getFinalSchemasClient(), ACCOUNT_NAME));
		assertEquals(0, getDocCount(adminHubClient.getFinalSchemasClient(), ACCOUNT_NAME2));

		assertEquals(0, getDocCount(adminHubClient.getStagingSchemasClient(), "http://marklogic.com/entity-services/models"));
		assertEquals(0, getDocCount(adminHubClient.getStagingSchemasClient(), "ml-data-hub-json-schema"));
		assertEquals(0, getDocCount(adminHubClient.getStagingSchemasClient(), "ml-data-hub-xml-schema"));

		assertNull(adminHubClient.getStagingClient().newDocumentManager().exists("/ingest/" + ACCOUNT_NAME + "/doc1.json"));
		assertNull(adminHubClient.getFinalClient().newDocumentManager().exists("/data/" + ACCOUNT_NAME + "/doc1.json"));
		assertNull(adminHubClient.getStagingClient().newDocumentManager().exists("/ingest/" + ACCOUNT_NAME2 + "/doc1.json"));
		assertNull(adminHubClient.getFinalClient().newDocumentManager().exists("/data/" + ACCOUNT_NAME2 + "/doc1.json"));
	}

	@Test
	void resetPassword() throws Exception {
		registerAccount();

		UserPojo beforeUser = userService.getUser(getFinalClient(), ACCOUNT_NAME);
		assertNull(beforeUser.token);
		assertNull(beforeUser.tokenExpiry);
		assertTrue(beforeUser.validated);
		assertNull(beforeUser.resetToken);
		assertNull(beforeUser.resetTokenExpiry);

		login();
		assertNotNull(authToken);

		logout();

		getJson(RESET_PASSWORD_URL + "?email=bob.smith@marklogic.com")
			.andExpect(status().isOk());
		assertNull(authToken);

		UserPojo finalUser = userService.getUser(getFinalClient(), ACCOUNT_NAME);
		assertNull(finalUser.token);
		assertNull(finalUser.tokenExpiry);
		assertTrue(finalUser.validated);
		assertNotNull(finalUser.resetToken);
		assertNotNull(finalUser.resetTokenExpiry);

		verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString());

		getJson(VALIDATE_RESET_TOKEN_URL + "?token=bogus")
			.andDo(
				result -> {
					JsonNode node = readJsonObject(result.getResponse().getContentAsString());
					assertTrue(node.get("email").isNull());
					assertFalse(node.get("valid").asBoolean());
					assertEquals("Invalid Token", node.get("error").asText());
				})
			.andExpect(status().isOk());
		assertNull(authToken);

		getJson(VALIDATE_RESET_TOKEN_URL + "?token=" + finalUser.resetToken)
			.andDo(
				result -> {
					JsonNode node = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(ACCOUNT_NAME, node.get("email").asText());
					assertTrue(node.get("valid").asBoolean());
					assertTrue(node.get("error").isNull());
				})
			.andExpect(status().isOk());
		assertNull(authToken);


		UpdatePasswordPojo resetPwd = new UpdatePasswordPojo();
		resetPwd.token = finalUser.resetToken;
		resetPwd.password = "newAwesomePassword";
		postJson(UPDATE_PASSWORD_URL, resetPwd)
			.andExpect(status().isOk());

		finalUser = userService.getUser(getFinalClient(), ACCOUNT_NAME);
		assertNull(finalUser.token);
		assertNull(finalUser.tokenExpiry);
		assertTrue(finalUser.validated);
		assertNull(finalUser.resetToken);
		assertNull(finalUser.resetTokenExpiry);

		assertNull(authToken);

		String payload = buildLoginPayload(ACCOUNT_NAME, ACCOUNT_PASSWORD);
		postJson(LOGIN_URL, payload)
			.andExpect(status().isUnauthorized());

		assertNull(authToken);

		loginAsUser(ACCOUNT_NAME, "newAwesomePassword");
		assertNotNull(authToken);
	}

	@Test
	void resetPasswordExpiredToken() throws Exception {
		registerAccount();

		UserPojo beforeUser = userService.getUser(getFinalClient(), ACCOUNT_NAME);
		assertNull(beforeUser.token);
		assertNull(beforeUser.tokenExpiry);
		assertTrue(beforeUser.validated);
		assertNull(beforeUser.resetToken);
		assertNull(beforeUser.resetTokenExpiry);

		login();
		assertNotNull(authToken);

		logout();

		getJson(RESET_PASSWORD_URL + "?email=bob.smith@marklogic.com")
			.andExpect(status().isOk());
		assertNull(authToken);

		UserPojo finalUser = userService.getUser(getFinalClient(), ACCOUNT_NAME);
		assertNull(finalUser.token);
		assertNull(finalUser.tokenExpiry);
		assertTrue(finalUser.validated);
		assertNotNull(finalUser.resetToken);
		assertNotNull(finalUser.resetTokenExpiry);

		verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString());

		// expire the token
		final Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, -2);
		finalUser.resetTokenExpiry = c.getTime();
		userService.saveUser(finalUser);

		getJson(VALIDATE_RESET_TOKEN_URL + "?token=" + finalUser.resetToken)
			.andDo(
				result -> {
					JsonNode node = readJsonObject(result.getResponse().getContentAsString());
					assertTrue(node.get("email").isNull());
					assertFalse(node.get("valid").asBoolean());
					assertEquals("Token Expired", node.get("error").asText());
				})
			.andExpect(status().isOk());
		assertNull(authToken);
	}

	@Test
	void getProfile() throws Exception {
		registerAccount();
		login();
		getJson(GET_PROFILE_URL)
			.andDo(
				result -> {
					JsonNode node = readJsonObject(result.getResponse().getContentAsString());
					assertEquals(ACCOUNT_NAME, node.get("email").asText());
					assertEquals("Bob Smith", node.get("name").asText());
				})
			.andExpect(status().isOk());
	}
}
