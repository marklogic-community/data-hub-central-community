package com.marklogic.envision.export;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.ext.datamovement.QueryBatcherJobTicket;
import com.marklogic.client.ext.datamovement.job.AbstractQueryBatcherJob;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class ExportToCsvFileJob extends AbstractQueryBatcherJob {
	private File exportFile;
	private String fileHeader;
	private String fileFooter;
	private StringWriter stringWriter;
	private ExportToCsvWriterListener exportToWriterListener;

	public ExportToCsvFileJob() {
		super();

		addJobProperty("transform", "Optional REST transform to apply to each record before it is written",
			value -> getExportListener().withTransform(new ServerTransform(value)));

		stringWriter = new StringWriter();
		this.exportToWriterListener = new ExportToCsvWriterListener(stringWriter);
		this.addUrisReadyListener(exportToWriterListener);

	}

	@Override
	public QueryBatcherJobTicket run(DatabaseClient databaseClient) {
		QueryBatcherJobTicket ticket = super.run(databaseClient);

		if (ticket.getQueryBatcher().isStopped()) {
			try {
				this.stringWriter.close();
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
	public StringWriter getStringWriter() {
		return stringWriter;
	}

	public File getExportFile() {
		return exportFile;
	}

	/**
	 * Allow client to fiddle with the ExportToWriterListener that's created by this class.
	 *
	 * @return
	 */
	public ExportToCsvWriterListener getExportListener() {
		return exportToWriterListener;
	}

	public void setFileHeader(String fileHeader) {
		this.fileHeader = fileHeader;
	}

	public void setFileFooter(String fileFooter) {
		this.fileFooter = fileFooter;
	}

	public void setTransform(String name) {
		getExportListener().withTransform(new ServerTransform(name));
	}
}

