package com.marklogic.r2m;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class R2MController {

	@Autowired
	private  R2MService r2MService;

	@PostMapping("/api/r2m")
	ResponseEntity<String> runR2M(@RequestBody R2MPayload payload) throws Exception {
		this.r2MService.execute(payload);
		return ResponseEntity.ok("Processing...");
	}
}
