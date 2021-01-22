package com.marklogic.envision.dataQuality;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.grove.boot.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data-profiler")
public class DataQualityController extends AbstractController {
	private final DataQualityService dataQualityService;

	@Autowired
	public DataQualityController(DataQualityService dataQualityService) {
		this.dataQualityService = dataQualityService;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	void profileData(@RequestBody ProfileDataParams params) {
		dataQualityService.profileDataAsync(getHubClient(), params.collection, params.database, params.sampleSize);
	}

	@RequestMapping(value = "/reports", method = RequestMethod.POST)
	JsonNode getReports(@RequestBody GetReportsParams params) {
		return dataQualityService.getReports(getHubClient(), params.page, params.pageLength);
	}

	@RequestMapping(value = "/report", method = RequestMethod.GET)
	JsonNode getReport(@RequestParam String uri) {
		return dataQualityService.getReport(getHubClient(), uri);
	}

	@RequestMapping(value = "/delete-all-reports", method = RequestMethod.GET)
	void deleteReport() {
		dataQualityService.deleteAllReports(getHubClient());
	}

	@RequestMapping(value = "/delete-report", method = RequestMethod.GET)
	void deleteReport(@RequestParam String uri) {
		dataQualityService.deleteReport(getHubClient(), uri);
	}
}
