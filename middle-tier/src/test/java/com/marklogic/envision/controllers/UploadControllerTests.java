package com.marklogic.envision.controllers;

import com.marklogic.envision.upload.UploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UploadControllerTests extends AbstractMvcTest {
	private static final String UPLOAD_URL = "/api/upload";

	@MockBean
	UploadService uploadService;

	@BeforeEach
	void setup() {
		logout();

		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();
	}

	@Test
	void upload() throws Exception {
		mockMvc.perform(buildUpload(UPLOAD_URL, new MockMultipartFile("file", "my-wacky-file.csv", "text/csv", getResourceStream("data/my-wacky-file.csv")))
			.param("collection", "my-wacky-file.csv"))
			.andExpect(status().isUnauthorized());

		registerAccount();
		login();

		mockMvc.perform(buildUpload(UPLOAD_URL, new MockMultipartFile("file", "my-wacky-file.csv", "text/csv", getResourceStream("data/my-wacky-file.csv")))
			.param("collection", "my-wacky-file.csv"))
			.andExpect(status().isOk());

		verify(uploadService, times(1)).asyncUploadFile(any(), any(), anyString());
	}
}
