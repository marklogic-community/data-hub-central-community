package com.marklogic.grove.boot.export;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ext.datamovement.QueryBatcherJobTicket;
import com.marklogic.grove.boot.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.marklogic.client.ext.datamovement.job.ExportToZipJob;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/export/")
public class ExportController extends AbstractController {
	@Autowired
	ExportController() {
	}

	// returns a zip package containing csv files
	@RequestMapping(value = "/runExports", method = RequestMethod.POST)
	public String runExports(@RequestBody List<String> entityNames)  {
		System.out.println("Exporting entities." + entityNames);
		ExportToZipJob exportToZipJob = new ExportToZipJob();
		//set the options for the job
		exportToZipJob.setExportFile(new File("/tmp/envision/exports/entities.zip"));
		//convert the string list we got to array for the job
		String[] entityArray = entityNames.toArray(new String[0]);
		exportToZipJob.setWhereCollections(entityArray);
		DatabaseClient client = getHubClient().getFinalClient();
		QueryBatcherJobTicket ticket = exportToZipJob.run(client);
		return "Exports run.";
	}
}
