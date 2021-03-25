package com.marklogic.envision.controllers;

import com.marklogic.envision.R2MConnector.R2MService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class R2MControllerTests extends AbstractMvcTest {
	private static final String UPLOAD_URL = "/api/r2mconnect";

	@MockBean
	R2MService r2mService;

	@BeforeEach
	void setup() {
		logout();

		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();
	}

	@Test
	void r2mconnect() throws Exception {
		mockMvc.perform(buildUpload(UPLOAD_URL, new MockMultipartFile("file", "my-wacky-file.csv", "text/csv", getResourceStream("data/my-wacky-file.csv")))
			.param("collection", "my-wacky-file.csv")
			.param("database", "staging"))
			.andExpect(status().isUnauthorized());

		registerAccount();
		login();

		mockMvc.perform(buildUpload(UPLOAD_URL, new MockMultipartFile("file", "my-wacky-file.csv", "text/csv", getResourceStream("data/my-wacky-file.csv")))
			.param("collection", "my-wacky-file.csv")
			.param("database", "staging"))
			.andExpect(status().isOk());

		verify(r2mService, times(1)).asyncRunConnection(any());

		reset(r2mService);

		mockMvc.perform(buildUpload(UPLOAD_URL, new MockMultipartFile("file", "my file with spaces.csv", "text/csv", getResourceStream("data/my-wacky-file.csv")))
			.param("collection", "MyCollection")
			.param("database", "staging"))
			.andExpect(status().isOk());

		verify(r2mService, times(1)).asyncRunConnection(any());
	}
}
