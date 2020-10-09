package com.marklogic.envision.export;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ext.datamovement.QueryBatcherJobTicket;
import com.marklogic.client.io.BytesHandle;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportService {

	@Async
	public void runExportsAsync(DatabaseClient client, List<String> entityNames)  {
		runExports(client, entityNames);
	}

	public void runExports(DatabaseClient client, List<String> entityNames)  {
		// TODO: create a zip stream at a temp file
//		ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File("/tmp/envision/exports/test.zip"))));

		entityNames.forEach(entityName -> {
			System.out.println("Exporting entities." + entityNames);
			ExportToCsvFileJob exportJob = new ExportToCsvFileJob();
			//set the options for the job
//			exportJob.setExportFile(new File("/tmp/envision/exports/test.csv"));
			//convert the string list we got to array for the job
			exportJob.setWhereCollections(entityName);
			QueryBatcherJobTicket ticket = exportJob.run(client);

			// TODO: get the ouput from the job. then insert it into the zip stream
			String output = exportJob.getStringWriter().toString();
//			zipOutputStream.putNextEntry(new ZipEntry(fileUriInsideZip)); // zipentry is a uri
//			zipOutputStream.write(convert output to byte array);
//			zipOutputStream.closeEntry();
		});
	}
}
