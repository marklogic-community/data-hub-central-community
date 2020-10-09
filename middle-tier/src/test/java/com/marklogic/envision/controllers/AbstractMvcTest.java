package com.marklogic.envision.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marklogic.envision.BaseTest;
import com.marklogic.envision.auth.UserPojo;
import com.marklogic.grove.boot.auth.LoginInfo;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
public class AbstractMvcTest extends BaseTest {


	@Autowired
	protected MockMvc mockMvc;

	protected String authToken;

	protected String buildLoginPayload(String username, String password) {
		LoginInfo loginInfo = new LoginInfo();
		loginInfo.username = username;
		loginInfo.password = password;
		try {
			return objectMapper.writeValueAsString(loginInfo);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	protected void registerAccount() throws IOException {
		registerAccount(ACCOUNT_NAME, ACCOUNT_PASSWORD);
	}

	protected void registerAccount(String username, String password) throws IOException {
		UserPojo user = new UserPojo();
		user.email = username;
		user.password = password;
		user.name = "Bob Smith";
		user = userService.createUser(user);
		userService.validateToken(user.token);
		Mockito.reset(emailService);
	}

	protected void registerEnvisionAdminAccount() throws IOException {
		UserPojo user = new UserPojo();
		user.email = ADMIN_ACCOUNT_NAME;
		user.password = ACCOUNT_PASSWORD;
		user.name = "Admin Smith";
		user = userService.createUser(user);
		userService.validateToken(user.token);
		getHubConfig().newStagingClient("Security").newServerEval().javascript(
			"declareUpdate(); \n" +
			"const sec = require('/MarkLogic/security.xqy');\n" +
			"sec.userAddRoles(\"" + ADMIN_ACCOUNT_NAME + "\", \"envisionAdmin\")"
		).eval();
		Mockito.reset(emailService);
	}

	protected void logout() {
		authToken = null;
	}

	protected ResultActions login() {
		return loginAsUser(ACCOUNT_NAME, ACCOUNT_PASSWORD);
	}

	protected ResultActions loginAsUser(String username, String password) {
		try {
			return postJson("/api/auth/login", buildLoginPayload(username, password))
				.andExpect(status().isOk())
				.andDo(result -> authToken = result.getResponse().getHeader("Authorization"));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	protected ResultActions postJson(String url, Object json) throws Exception {
		return postJson(url, objectMapper.valueToTree(json).toString());
	}

	protected MockHttpServletRequestBuilder buildJsonPost(String url, String json) {
		MockHttpServletRequestBuilder builder = post(url).contentType(MediaType.APPLICATION_JSON).content(json);
		if (authToken != null) {
			builder.header("Authorization", authToken);
		}
		return builder;
	}

	protected ResultActions postJson(String url, String json) throws Exception {
		return mockMvc.perform(buildJsonPost(url, json));
	}

	protected MockHttpServletRequestBuilder buildUpload(String url, MockMultipartFile file) {
		MockHttpServletRequestBuilder builder = multipart(url).file(file);
		if (authToken != null) {
			builder.header("Authorization", authToken);
		}
		return builder;
	}

	protected ResultActions putJson(String url, Object json) throws Exception {
		return putJson(url, objectMapper.valueToTree(json).toString());
	}

	protected MockHttpServletRequestBuilder buildJsonPut(String url, String json) {
		MockHttpServletRequestBuilder builder = put(url).contentType(MediaType.APPLICATION_JSON).content(json);
		if (authToken != null) {
			builder.header("Authorization", authToken);
		}
		return builder;
	}

	protected ResultActions putJson(String url, String json) throws Exception {
		return mockMvc.perform(buildJsonPut(url, json));
	}

	protected ResultActions getJson(String url) throws Exception {
		return getJson(url, new LinkedMultiValueMap<>());
	}

	protected ResultActions getJson(String url, MultiValueMap<String, String> params) throws Exception {
		MockHttpServletRequestBuilder builder = get(url).params(params);
		if (authToken != null) {
			builder.header("Authorization", authToken);
		}
		return mockMvc.perform(builder);
	}
}
