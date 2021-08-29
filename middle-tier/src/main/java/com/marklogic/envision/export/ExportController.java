package com.marklogic.envision.export;

import com.marklogic.grove.boot.AbstractController;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
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

	@RequestMapping(value = "/getExports", method = RequestMethod.GET)
	public List<ExportInfo> getExports() {
		return exportService.getExports(getHubClient());
	}

	@RequestMapping(value = "/downloadExport", method = RequestMethod.GET)
	void downloadExport(@RequestParam String exportId, HttpServletResponse response) throws IOException {
		File exportJobsFile = exportService.getFile(getHubClient().getFinalClient(), getHubClient().getUsername(), exportId);
		response.setContentType("application/zip");
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename =\"" + exportJobsFile.getName() + "\"");
		InputStream inputStream = new FileInputStream(exportJobsFile);
		IOUtils.copy(inputStream, response.getOutputStream());
	}

	@RequestMapping(value = "/deleteExport", method = RequestMethod.GET)
	void deleteExport(@RequestParam String exportId) throws IOException {
		exportService.removeExport(getHubClient().getFinalClient(), getHubClient().getUsername(), exportId);
	}
}
