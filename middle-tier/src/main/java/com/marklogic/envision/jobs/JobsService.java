package com.marklogic.envision.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.Jobs;
import org.springframework.stereotype.Service;

@Service
public class JobsService {

	public JsonNode getJobs(DatabaseClient client, String flowName) {
		return Jobs.on(client).getJobs(flowName);
	}

	public void deleteJob(DatabaseClient client, String jobId) {
		Jobs.on(client).deleteJob(jobId);
	}
}
