package com.marklogic.envision.export;

import com.marklogic.client.DatabaseClient;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportService {

	@Async
	public void runExportsAsync(DatabaseClient client, String username, List<String> entityNames) throws IOException {
		runExports(client, username, entityNames);
	}

	public void runExports(DatabaseClient client, String username, List<String> entityNames) throws IOException {
		File zipFile = File.createTempFile("export", ".zip");
		ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

		for (int i = 0; i < entityNames.size(); i++) {
			String entityName = entityNames.get(i);
			try {
				ExportToCsvFileJob exportJob = new ExportToCsvFileJob();
				exportJob.setWhereCollections(entityName);
				exportJob.run(client);

				File exportFile = exportJob.getExportFile();
				zipOutputStream.putNextEntry(new ZipEntry(entityName + ".csv"));
				zipOutputStream.write(FileUtils.readFileToByteArray(exportFile));
				zipOutputStream.closeEntry();
				exportFile.delete();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		zipOutputStream.close();

		// TODO: do stuff with the zip file here
		System.out.println(zipFile.getAbsolutePath());
	}
}
