package com.marklogic.envision.upload;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.datamovement.DataMovementManager;
import com.marklogic.client.datamovement.JacksonCSVSplitter;
import com.marklogic.client.datamovement.WriteBatcher;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.client.io.*;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.pojo.StatusMessage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.lang.PipedQuadsStream;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.sparql.core.Quad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class UploadService extends LoggingObject {

	final static private int MAXTRIPLESPERDOCUMENT = 100;
	protected Random random;
	protected long randomValue;
	protected long milliSecs;
	protected static Pattern[] patterns = new Pattern[] {
		Pattern.compile("&"), Pattern.compile("<"), Pattern.compile(">") };


	final private SimpMessagingTemplate template;
	private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	@Autowired
	UploadService(SimpMessagingTemplate template) {
		this.template = template;
		random = new Random();
		randomValue = random.nextLong();
		Calendar cal = Calendar.getInstance();
		milliSecs = cal.getTimeInMillis();
	}

	@Async
	public void asyncUploadFiles(HubClient client, UploadFile[] files, String database, String collectionName) {
		uploadFiles(client, files, database, collectionName);
	}

	protected String cleanSpaces(String input) {
		return input.replaceAll("\\s+", "_");
	}

	private boolean isTriple(String fileName) {
		return (fileName.toLowerCase().endsWith("rdf") || fileName.toLowerCase().endsWith("ttl") || fileName.toLowerCase().endsWith("n3") || fileName.toLowerCase().endsWith("nt") || fileName.toLowerCase().endsWith("nq") || fileName.toLowerCase().endsWith("trig"));
	}

	public void uploadFiles(HubClient client, UploadFile[] files, String database, String collectionName) {
		String username = client.getHubConfig().getMlUsername();

		UploadFile[] triples = Arrays.stream(files).filter(file -> isTriple(file.getFileName())).toArray(UploadFile[]::new);
		UploadFile[] nonTriples = Arrays.stream(files).filter(file -> !isTriple(file.getFileName())).toArray(UploadFile[]::new);

		// load the triples in their own batch w/o running envelope
		if (triples.length > 0) {
			uploadFiles(client, database, collectionName, false, writeBatcher -> {
				AtomicInteger filesWritten = new AtomicInteger(0);
				Arrays.stream(triples).forEach(file -> {
					String fileName = file.getFileName();
					InputStream is = file.getInputStream();
					String prefix = String.format("/ingest/%s/%s/%s", username, collectionName, fileName);
					if (collectionName.equals(fileName)) {
						prefix = String.format("/ingest/%s/%s", username, collectionName);
					}
					filesWritten.getAndAdd(uploadTriples(writeBatcher, fileName, is, prefix));
				});

				return filesWritten.get();
			});
		}

		if (nonTriples.length > 0) {
			uploadFiles(client, database, collectionName, true, writeBatcher -> {
				AtomicInteger filesWritten = new AtomicInteger(0);
				Arrays.stream(nonTriples).forEach(file -> {
					String fileName = file.getFileName();
					InputStream is = file.getInputStream();
					String prefix = String.format("/ingest/%s/%s/%s", username, collectionName, fileName);
					if (collectionName.equals(fileName)) {
						prefix = String.format("/ingest/%s/%s", username, collectionName);
					}
					if (fileName.toLowerCase().endsWith("csv") || fileName.toLowerCase().endsWith("psv")) {
						filesWritten.getAndAdd(uploadCsvFile(writeBatcher, is, prefix));
					} else if (fileName.toLowerCase().endsWith("zip")) {
						filesWritten.getAndAdd(uploadZip(writeBatcher, is, prefix));
					} else {
						InputStreamHandle handle = new InputStreamHandle(is);
						if (fileName.toLowerCase().endsWith("xml")) {
							handle.withFormat(Format.XML);
						} else if (fileName.toLowerCase().endsWith("json")) {
							handle.withFormat(Format.JSON);
						}
						writeBatcher.add(cleanSpaces(prefix), handle);
						filesWritten.addAndGet(1);
					}

				});

				return filesWritten.get();
			});
		}
	}

	@SuppressWarnings("unchecked")
	public int uploadTriples(WriteBatcher writeBatcher, String fileName, InputStream stream, String prefix) {
		AtomicInteger filesWritten = new AtomicInteger(0);

		Lang lang;
		if (fileName.toLowerCase().endsWith("rdf")) {
			lang = RDFLanguages.RDFXML;
		}
		else if (fileName.toLowerCase().endsWith("ttl")) {
			lang = RDFLanguages.TURTLE;
		}
		else if (fileName.toLowerCase().endsWith("n3")) {
			lang = RDFLanguages.N3;
		}
		else if (fileName.toLowerCase().endsWith("nt")) {
			lang = RDFLanguages.NTRIPLES;
		}
		else if (fileName.toLowerCase().endsWith("nq")) {
			lang = RDFLanguages.NQUADS;
		}
		else if (fileName.toLowerCase().endsWith("trig")) {
			lang = RDFLanguages.TRIG;
		}
		else {
			lang = RDFLanguages.RDFXML;
		}

		PipedRDFIterator rdfIter;
		PipedRDFStream rdfInputStream;
		if (lang == Lang.NQUADS || lang == Lang.TRIG) {
			rdfIter = new PipedRDFIterator<Quad>();
			rdfInputStream = new PipedQuadsStream(rdfIter);
		} else {
			rdfIter = new PipedRDFIterator<Triple>();
			rdfInputStream = new PipedTriplesStream(rdfIter);
		}

		Parser parser = new Parser(stream, lang, rdfInputStream);

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(parser);

		while (rdfIter.hasNext()) {
			StringBuilder buffer = new StringBuilder();
			buffer.append("<sem:triples xmlns:sem='http://marklogic.com/semantics'>");
			int max = MAXTRIPLESPERDOCUMENT;
			while (max > 0 && rdfIter.hasNext()) {
				Object next = rdfIter.next();
				Triple triple;
				if (next instanceof Quad) {
					triple = ((Quad) next).asTriple();
				}
				else {
					triple = (Triple)next;
				}
				buffer.append("<sem:triple>");
				buffer.append(subject(triple.getSubject()));
				buffer.append(predicate(triple.getPredicate()));
				buffer.append(object(triple.getObject()));
				buffer.append("</sem:triple>");
				max--;
			}
			buffer.append("</sem:triples>\n");

			String uri = String.format("%s/%s.xml", prefix, UUID.randomUUID());
			writeBatcher.add(cleanSpaces(uri), new StringHandle(buffer.toString()));
			filesWritten.getAndAdd(1);
		}

		executor.shutdown();
		return filesWritten.get();
	}

	private long rotl(long x, long y) {
		return (x<<y)^(x>>(64-y));
	}

	private long fuse(long a, long b)  {
		return rotl(a,8)^b;
	}

	private long scramble(long x) {
		return x^rotl(x,20)^rotl(x,40);
	}

	private long hash64(long value, String str) {
		char[] arr = str.toCharArray();
		for (int i = 0; i < str.length(); i++) {
			long HASH64_STEP = 15485863L;
			value = (value + Character.getNumericValue(arr[i])) * HASH64_STEP;
		}
		return value;
	}

	protected String resource(Node rsrc) {
		if (rsrc.isBlank()) {
			return "http://marklogic.com/semantics/blank/" + Long.toHexString(
				hash64(fuse(scramble(milliSecs),randomValue), rsrc.getBlankNodeLabel()));
		} else {
			return escapeXml(rsrc.toString());
		}
	}

	protected String resource(Node rsrc, String tag) {
		String uri = resource(rsrc);
		return "<sem:" + tag + ">" + uri + "</sem:" + tag + ">";
	}

	protected String subject(Node subj) {
		return resource(subj, "subject");
	}

	protected String predicate(Node subj) {
		return resource(subj, "predicate");
	}

	protected String object(Node node) {
		if (node.isLiteral()) {
			String text = node.getLiteralLexicalForm();
			String type = node.getLiteralDatatypeURI();
			String lang = node.getLiteralLanguage();

			if (lang == null || "".equals(lang)) {
				lang = "";
			} else {
				lang = " xml:lang='" + escapeXml(lang) + "'";
			}

			if ("".equals(lang)) {
				if (type == null) {
					type = "http://www.w3.org/2001/XMLSchema#string";
				}
				type = " datatype='" + escapeXml(type) + "'";
			} else {
				type = "";
			}

			return "<sem:object" + type + lang + ">" + escapeXml(text) + "</sem:object>";
		} else if (node.isBlank()) {
			return "<sem:object>http://marklogic.com/semantics/blank/" + Long.toHexString(
				hash64(fuse(scramble(milliSecs),randomValue), node.getBlankNodeLabel()))
				+"</sem:object>";
		} else {
			return "<sem:object>" + escapeXml(node.toString()) + "</sem:object>";
		}
	}

	protected static String escapeXml(String _in) {
		if (null == _in){
			return "";
		}
		return patterns[2].matcher(
			patterns[1].matcher(
				patterns[0].matcher(_in).replaceAll("&amp;"))
				.replaceAll("&lt;")).replaceAll("&gt;");
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
							InputStreamHandle handle = new InputStreamHandle(fileInputStream);
							if (fileName.toLowerCase().endsWith("xml")) {
								handle.withFormat(Format.XML);
							} else if (fileName.toLowerCase().endsWith("json")) {
								handle.withFormat(Format.JSON);
							}
							String uri = String.format("%s/%s", prefix, fileName);
							writeBatcher.add(cleanSpaces(uri), handle);
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

	private void uploadFiles(HubClient client, String database, String collectionName, boolean envelope, Function<WriteBatcher, Integer> fileIterator) {
		DatabaseClient databaseClient;
		if (database.equals("staging")) {
			databaseClient = client.getStagingClient();
		}
		else {
			databaseClient = client.getFinalClient();
		}
		DataMovementManager dataMovementManager = databaseClient.newDataMovementManager();

		String jobId = UUID.randomUUID().toString();
		ServerTransform serverTransform = new ServerTransform("wrapEnvelope");
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
			.withJobId(jobId);

		if (envelope) {
			writeBatcher.withTransform(serverTransform);
		}

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
				writeBatcher.add(cleanSpaces(uri), jacksonHandle);
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

	protected static class Parser implements Runnable
	{
		final PipedRDFStream rdfStream;
		final RDFParserBuilder builder;

		public Parser(InputStream stream, Lang lang, PipedRDFStream rdfStream)
		{
			this.rdfStream = rdfStream;
			builder = RDFParser.create()
				.source(stream)
				.lang(lang);
		}

		public void setLang( Lang lang ) { builder.lang(lang); }

		public void setErrorHandler( ErrorHandler handler ) { builder.errorHandler(handler); }

		@Override
		public void run() { builder.build().parse(rdfStream); }

	}
}
