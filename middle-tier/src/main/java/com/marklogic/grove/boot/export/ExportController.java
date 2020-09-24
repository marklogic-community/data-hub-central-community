package com.marklogic.grove.boot.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export/")
public class ExportController {
	@Autowired
	ExportController() {
	}

	// returns a zip package containing csv files
	@RequestMapping(value = "/runExports", method = RequestMethod.POST)
	public String runExports()  {
		System.out.println("Exporting entities.");
		return "Exports run.";
	}
}
