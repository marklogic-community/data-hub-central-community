package com.marklogic.envision.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.envision.BaseTest;
import com.marklogic.envision.auth.UserPojo;
import com.marklogic.envision.auth.UserService;
import com.marklogic.envision.email.EmailService;
import com.marklogic.grove.boot.auth.LoginInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
public class AbstractMvcTest extends BaseTest {
	public static final String ACCOUNT_NAME = "bob.smith@marklogic.com";
	public static final String ACCOUNT_PASSWORD = "password";

	@MockBean
	EmailService emailService;

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected UserService userService;

	@BeforeAll
	static void instal() {

	}
	@BeforeEach
	void setup() throws IOException {
		MockitoAnnotations.initMocks(this);
	}

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
		UserPojo user = new UserPojo();
		user.email = ACCOUNT_NAME;
		user.password = ACCOUNT_PASSWORD;
		user.name = "Bob Smith";
		user = userService.createUser(user, "");
		userService.validateToken(user.token);
		Mockito.reset(emailService);
	}

	protected void logout() {
		authToken = null;
	}

	protected ResultActions loginAsUser(String username, String password) {
		try {
			return postJson("/api/auth/login", buildLoginPayload(username, password))
				.andExpect(status().isOk())
				.andDo(result -> {
					authToken = result.getResponse().getHeader("Authorization");
				});
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

	protected ObjectNode readJsonObject(String json) {
		try {
			return (ObjectNode) objectMapper.readTree(json);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
