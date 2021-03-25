package com.marklogic.envision.R2MConnector;

import com.marklogic.grove.boot.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/r2mconnect")
public class R2MController extends AbstractController {

	final private R2MService r2mService;
	private String database;

	@Autowired
	R2MController(R2MService r2MService) {
		this.r2mService = r2MService;
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> runDBConnector(@RequestParam("database") String database) {

		this.database = database;
		this.r2mService.asyncRunConnection(getHubClient());
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
