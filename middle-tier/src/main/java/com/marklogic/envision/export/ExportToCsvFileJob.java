package com.marklogic.envision.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ext.datamovement.QueryBatcherJobTicket;
import com.marklogic.client.ext.datamovement.job.AbstractQueryBatcherJob;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExportToCsvFileJob extends AbstractQueryBatcherJob {
	private File exportFile;
	private FileWriter fileWriter;
	private ExportToCsvWriterListener exportToWriterListener;
	private String entityName;

	public ExportToCsvFileJob(JsonNode model, String entityName) {
		super();

		this.entityName = entityName;
		this.setWhereCollections(entityName);
		this.setConsistentSnapshot(false);

		try {
			exportFile = File.createTempFile("export", "");
			this.fileWriter = new FileWriter(exportFile);
			this.exportToWriterListener = new ExportToCsvWriterListener(fileWriter, model);
			this.addUrisReadyListener(exportToWriterListener);
		} catch (IOException ie) {
			throw new RuntimeException("Unable to open FileWriter on file: " + exportFile + "; cause: " + ie.getMessage(), ie);
		}
	}

	@Override
	public QueryBatcherJobTicket run(DatabaseClient databaseClient) {
		exportToWriterListener.setEntityName(entityName);
		QueryBatcherJobTicket ticket = super.run(databaseClient);

		if (ticket.getQueryBatcher().isStopped()) {
			try {
				this.fileWriter.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return ticket;
	}

	@Override
	protected String getJobDescription() {
		return "Exporting documents " + getQueryDescription() + " to file at: " + exportFile;
	}

	/**
	 * Allow client to fiddle with the FileWriter that's created by this class.
	 *
	 * @return
	 */
	public FileWriter getFileWriter() {
		return fileWriter;
	}

	public File getExportFile() {
		return exportFile;
	}
}

