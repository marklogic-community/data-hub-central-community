package com.marklogic.envision.controllers;

import com.marklogic.envision.R2MConnector.R2MService;

import com.marklogic.envision.hub.HubClient;
import com.marklogic.r2m.R2MPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class R2MControllerTests extends AbstractMvcTest {
	private static final String UPLOAD_URL = "/api/r2mconnect";

	@Autowired
	private R2MService r2mService;
/*
	@BeforeEach
	public void setup() throws IOException {
		super.setup();

		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
	}
*/
	@Test
	void r2mconnect() throws Exception {
		try {
			R2MPayload payload = new R2MPayload();
			//TODO load up the payload with the test config
			r2mService.execute( payload);
			//verify(r2mService, times(1)).asyncRunConnection(hubClient);
		} catch (Exception e) {
			throw new Exception("Unable to test r2m, cause: " + e.getMessage(), e);
		}
	}
}
