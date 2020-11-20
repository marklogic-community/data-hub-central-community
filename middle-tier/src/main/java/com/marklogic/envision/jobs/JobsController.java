package com.marklogic.envision.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.grove.boot.AbstractController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobsController extends AbstractController {

	private final JobsService jobsService;

	public JobsController(JobsService jobsService) {
		this.jobsService = jobsService;
	}

	@RequestMapping(method = RequestMethod.GET)
	JsonNode getJobs(@RequestParam String flowName) {
		return jobsService.getJobs(getHubClient().getJobsClient(), flowName);
	}

	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	void deleteJob(@RequestParam String jobId) {
		jobsService.deleteJob(getHubClient().getJobsClient(), jobId);
	}
}
