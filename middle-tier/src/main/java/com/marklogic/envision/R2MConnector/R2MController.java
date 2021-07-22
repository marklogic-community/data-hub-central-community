package com.marklogic.envision.R2MConnector;

import com.marklogic.grove.boot.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.marklogic.r2m.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/r2m")
public class R2MController extends AbstractController {

	@Autowired
	private R2MService r2MService;

	public static final int NUM_THREADS_PER_HOST = 5;
	public static final int BATCH_SIZE = 10;
	public static final String AUTH_CONTEXT = "digest";

	@RequestMapping(value = "/runExports", method = RequestMethod.POST)
	ResponseEntity<String> runR2M(@RequestBody R2MPayload payload) throws Exception {
		MarkLogicConfiguration markLogicConfiguration = new MarkLogicConfiguration(
			Collections.singletonList(getHubClient().getHubConfig().getHost()),
			getHubClient().getHubConfig().getStagingPort(),
			NUM_THREADS_PER_HOST,
			BATCH_SIZE,
			getHubClient().getHubConfig().getMlUsername(),
			getHubClient().getHubConfig().getMlPassword(),
			AUTH_CONTEXT
		);

		payload.setMlConfig(markLogicConfiguration);
		try {
			this.r2MService.execute(payload);
			return ResponseEntity.ok("Processing...");
		} catch (Exception e) {
			return ResponseEntity.ok("Unable to run r2m, cause: " + e.getMessage());
		}
	}
}
