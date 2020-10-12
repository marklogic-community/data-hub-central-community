package com.marklogic.envision.export;

import com.marklogic.grove.boot.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/export")
public class ExportController extends AbstractController {

	private final ExportService exportService;

	@Autowired
	ExportController(ExportService exportService) {
		this.exportService = exportService;
	}

	@RequestMapping(value = "/runExports", method = RequestMethod.POST)
	public void runExports(@RequestBody List<String> entityNames) throws IOException {
		exportService.runExportsAsync(getHubClient().getFinalClient(), getHubClient().getUsername(), entityNames);
	}
}
