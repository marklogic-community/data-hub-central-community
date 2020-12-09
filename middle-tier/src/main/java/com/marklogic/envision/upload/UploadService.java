package com.marklogic.envision.upload;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.marklogic.client.datamovement.DataMovementManager;
import com.marklogic.client.datamovement.JacksonCSVSplitter;
import com.marklogic.client.datamovement.WriteBatcher;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.pojo.StatusMessage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class UploadService extends LoggingObject {

	final private SimpMessagingTemplate template;
	private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	@Autowired
	UploadService(SimpMessagingTemplate template) {
		this.template = template;
	}

	@Async
	public void asyncUploadFiles(HubClient client, UploadFile[] files, String collectionName) {
		uploadFiles(client, files, collectionName);
	}

	public void uploadFiles(HubClient client, UploadFile[] files, String collectionName) {
		String username = client.getHubConfig().getMlUsername();
		uploadFiles(client, collectionName, writeBatcher -> {
			AtomicInteger filesWritten = new AtomicInteger(0);
			filesWritten.addAndGet(1);
			Arrays.stream(files).forEach(file -> {
				String fileName = file.getFileName();
				InputStream is = file.getInputStream();
				String prefix = String.format("/ingest/%s/%s/%s", username, collectionName, fileName);
				if (collectionName.equals(fileName)) {
					prefix = String.format("/ingest/%s/%s", username, collectionName);
				}
				if (fileName.toLowerCase().endsWith("csv") || fileName.toLowerCase().endsWith("psv")) {
					filesWritten.getAndAdd(uploadCsvFile(writeBatcher, is, prefix));
				}
				else if (fileName.toLowerCase().endsWith("zip")) {
					filesWritten.getAndAdd(uploadZip(writeBatcher, is, prefix));
				}
				else {
					writeBatcher.add(prefix, new InputStreamHandle(is));
				}
			});

			return filesWritten.get();
		});
	}

	public int uploadZip(WriteBatcher writeBatcher, InputStream stream, String prefix) {
		AtomicInteger filesWritten = new AtomicInteger(0);
		try {
			try (BufferedInputStream bis = new BufferedInputStream(stream);
				 ZipInputStream zis = new ZipInputStream(bis)) {

				ZipEntry ze;
				while ((ze = zis.getNextEntry()) != null) {
					String fileName = ze.getName();
					if (!ze.isDirectory() && !fileName.toLowerCase().contains("__macosx") && !fileName.contains(".DS_Store")) {
						InputStream fileInputStream = readZipFileContents(zis);
						String newPrefix = String.format("%s/%s", prefix, fileName);
						if (fileName.toLowerCase().endsWith("csv") || fileName.toLowerCase().endsWith("psv")) {
							filesWritten.getAndAdd(uploadCsvFile(writeBatcher, fileInputStream, newPrefix));
						} else if (fileName.toLowerCase().endsWith("zip")) {
							filesWritten.getAndAdd(uploadZip(writeBatcher, fileInputStream, newPrefix));
						} else {
							String uri = String.format("%s/%s", prefix, fileName);
							writeBatcher.add(uri, new InputStreamHandle(fileInputStream));
							filesWritten.getAndAdd(1);
						}
					}
				}
			}
		} catch (Exception e) {
			IOUtils.closeQuietly(stream);
			throw new RuntimeException(e);
		}
		return filesWritten.get();
	}

	private InputStream readZipFileContents(InputStream is) throws IOException {
		final byte[] contents = new byte[1024];
		int bytesRead;
		ByteArrayOutputStream streamBuilder = new ByteArrayOutputStream();
		while ((bytesRead = is.read(contents)) >= 0) {
			streamBuilder.write(contents, 0, bytesRead);
		}
		return new ByteArrayInputStream(streamBuilder.toByteArray());
	}

	public void uploadFiles(HubClient client, String collectionName, Function<WriteBatcher, Integer> fileIterator) {
		DataMovementManager dataMovementManager = client.getStagingClient().newDataMovementManager();
		ServerTransform serverTransform = new ServerTransform("wrapEnvelope");
		String jobId = UUID.randomUUID().toString();
		serverTransform.addParameter("job-id", jobId);
		String step = "1";
		serverTransform.addParameter("step", step);

		DocumentMetadataHandle metadataHandle = new DocumentMetadataHandle();
		metadataHandle.withCollections(collectionName);
		metadataHandle.getPermissions().clear();
		if (client.isMultiTenant()) {
			String roleName = DigestUtils.md5Hex(client.getUsername());
			metadataHandle.withPermission(roleName, DocumentMetadataHandle.Capability.READ, DocumentMetadataHandle.Capability.UPDATE);
		}
		DocumentMetadataHandle.DocumentMetadataValues metadataValues = metadataHandle.getMetadataValues();
		metadataValues.add("datahubCreatedByJob", jobId);
		metadataValues.add("datahubCreatedOn", DATE_TIME_FORMAT.format(new Date()));
		metadataValues.add("datahubCreatedBy", client.getHubConfig().getMlUsername());

		int threadCount = 4;
		int batchSize = 256;
		WriteBatcher writeBatcher = dataMovementManager.newWriteBatcher()
			.withDefaultMetadata(metadataHandle)
			.withBatchSize(batchSize)
			.withThreadCount(threadCount)
			.withJobId(jobId)
			.withTransform(serverTransform);

		StatusMessage msg = StatusMessage.newStatus(collectionName);

		try {
			if (!writeBatcher.isStopped()) {
				AtomicLong totalUris = new AtomicLong(0);
				updateStatus(msg.withMessage("Splitting " + collectionName + "..."));
				long startTime = System.nanoTime();

				totalUris.addAndGet(fileIterator.apply(writeBatcher));

				long endTime = System.nanoTime();
				long duration = (endTime - startTime);
				logger.error("Duration: " + (duration / 1000000) + " ms");
				updateStatus(
					msg.withMessage("Processing " + collectionName + "...")
						.withPercentComplete(0)
				);
				writeBatcher.onBatchSuccess(batch -> {
					long completed = batch.getJobWritesSoFar();
					int percentComplete = (int) (((double)completed / totalUris.get()) * 100.0);
					updateStatus(msg.withPercentComplete(percentComplete));
				})
				.onBatchFailure((batch, ex) -> {
					long completed = batch.getJobWritesSoFar();
					int percentComplete = (int) (((double)completed / totalUris.get()) * 100.0);
					updateStatus(msg.withError(ex.getMessage()).withPercentComplete(percentComplete));
				});
				writeBatcher.flushAndWait();
				dataMovementManager.stopJob(writeBatcher);
				updateStatus(msg.withPercentComplete(100));
			}
		} catch (Exception e) {
			updateStatus(msg.withError(e.getMessage()));
			throw new RuntimeException(e);
		}
	}

	public int uploadCsvFile(WriteBatcher writeBatcher, InputStream stream, String prefix) {
		BOMInputStream bis = new BOMInputStream(stream, false);
		CsvSchema schema = buildSchema(bis);
		AtomicInteger filesWritten = new AtomicInteger(0);
		try {
			JacksonCSVSplitter splitter = new JacksonCSVSplitter().withCsvSchema(schema);
			Stream<JacksonHandle> contentStream = splitter.split(bis);
			contentStream.forEach(jacksonHandle -> {
				String uri = String.format("%s/%s.json", prefix, UUID.randomUUID());
				writeBatcher.add(uri, jacksonHandle);
				filesWritten.addAndGet(1);
			});
		}
		catch (Exception e) {
			IOUtils.closeQuietly(stream);
			throw new RuntimeException(e);
		}
		return filesWritten.get();
	}

	private void updateStatus(StatusMessage message) {
		template.convertAndSend("/topic/status", message);
	}

	private CsvSchema buildSchema(InputStream stream) {
		String delimiter = ",";
		try {

			StringBuilder headerLineBuilder = new StringBuilder();
			int data = stream.read();
			while(data != -1) {
				char theChar = (char)data;
				if (theChar == '\n') {
					break;
				}
				if (theChar != '\r') {
					headerLineBuilder.append(theChar);
				}
				data = stream.read();
			}

			String headerLine = headerLineBuilder.toString();

			// assume one of following delimiters
			Map<String, Integer> counts = new HashMap<>();
			List<String> possibleDelimiters = Arrays.asList(",", ";", "\t", "|");
			String tmpHeader = headerLine;
			for (String del : possibleDelimiters) {
				int index = tmpHeader.indexOf(del);
				int count = 0;
				while (index != -1) {
					count++;
					tmpHeader = tmpHeader.substring(index + 1);
					index = tmpHeader.indexOf(del);
				}
				counts.put(del, count);
			}

			Map.Entry<String, Integer> maxEntry = null;

			for (Map.Entry<String, Integer> entry : counts.entrySet()) {
				if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
					maxEntry = entry;
				}
			}
			if (maxEntry != null) {
				delimiter = maxEntry.getKey();
			}

			String[] headers = headerLine.split(Pattern.quote(delimiter));
			CsvSchema.Builder builder = CsvSchema.builder();
			for (String header : headers) {
				builder.addColumn(header);
			}
			return builder.setQuoteChar('"')
				.setUseHeader(false)
				.setColumnSeparator(delimiter.charAt(0))
				.build();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return CsvSchema.emptySchema().withColumnSeparator(delimiter.charAt(0));
	}
}
