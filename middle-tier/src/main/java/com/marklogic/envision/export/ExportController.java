package com.marklogic.envision.export;

import com.marklogic.grove.boot.AbstractController;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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

	@RequestMapping(method = RequestMethod.GET)
	public List<ExportInfo> getExports() {
		return exportService.getExports(getHubClient());
	}

	@RequestMapping(value = "/{exportId}", method = RequestMethod.GET)
	void downloadExport(@PathVariable String exportId, HttpServletResponse response) throws IOException {
		response.setContentType("application/zip");
		InputStream inputStream = exportService.getFile(getHubClient().getFinalClient(), getHubClient().getUsername(), exportId);
		IOUtils.copy(inputStream, response.getOutputStream());
	}
}
