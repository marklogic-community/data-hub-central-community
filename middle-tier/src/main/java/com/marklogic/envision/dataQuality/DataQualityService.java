package com.marklogic.envision.dataQuality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.dataQuality.dataServices.DataQuality;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.pojo.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
public class DataQualityService {

	private final int DEFAULT_BATCH_SIZE = 100;
	private final int DEFAULT_THREAD_COUNT = 4;
	private final int DEFAULT_PAGE_LENGTH = 100;
	private final ObjectMapper objectMapper = new ObjectMapper();
	final private SimpMessagingTemplate template;
	private ExecutorService threadPool;

	@Autowired
	public DataQualityService(SimpMessagingTemplate template) {
		this.template = template;
	}

	@Async
	public void profileDataAsync(HubClient hubClient, String collection, String database, int sampleSize) {
		profileData(hubClient, collection, database, sampleSize);
	}

	public void profileData(HubClient hubClient, String collection, String database, int sampleSize) {
		threadPool = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);

		DatabaseClient databaseClient;
		if (database.equals("staging")) {
			databaseClient = hubClient.getStagingClient();
		}
		else {
			databaseClient = hubClient.getFinalClient();
		}

		DataQuality dq = DataQuality.on(databaseClient);
		JsonNode dataModel = dq.getDataModel(collection, sampleSize);

		List<JsonNode> mergeUs = Collections.synchronizedList(new ArrayList<>());
		AtomicReference<JsonNode> merged = new AtomicReference<>(objectMapper.createObjectNode());
		threadPool.execute(new GetUrisTask(databaseClient, collection, 1, DEFAULT_PAGE_LENGTH, sampleSize, DEFAULT_BATCH_SIZE, uris -> {
			synchronized (mergeUs) {
				mergeUs.add(dq.getDataProfile(dataModel, objectMapper.valueToTree(uris)));
				if (mergeUs.size() >= 10) {
					List<JsonNode> toMerge = new ArrayList<>(mergeUs);
					mergeUs.clear();
					merged.updateAndGet(jsonNode -> {
						toMerge.add(0, jsonNode);
						JsonNode mergedDoc = dq.mergeDataProfiles(objectMapper.valueToTree(toMerge));
						return mergedDoc;
					});
				}
			}
		}));

		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch(InterruptedException e) {}
		threadPool.shutdown();

		if (mergeUs.size() > 0) {
			List<JsonNode> toMerge = new ArrayList<>(mergeUs);
			merged.updateAndGet(jsonNode -> {
				toMerge.add(0, jsonNode);
				JsonNode mergedDoc = dq.mergeDataProfiles(objectMapper.valueToTree(toMerge));
				return mergedDoc;
			});
		}

		DataQuality.on(hubClient.getStagingClient()).saveReport(dataModel, merged.get(), "/data-quality/report", null);
		this.template.convertAndSend("/topic/status", StatusMessage.newStatus(collection).withMessage("Profile Complete").withPercentComplete(100));
	}

	JsonNode getReports(HubClient hubClient, int page, int pageLength) {
		return DataQuality.on(hubClient.getStagingClient()).getReports(Math.max(1, page), pageLength);
	}

	JsonNode getReport(HubClient hubClient, String uri) {
		return hubClient.getStagingClient().newJSONDocumentManager().readAs(uri, JsonNode.class);
	}

	void deleteAllReports(HubClient hubClient) {
		DataQuality.on(hubClient.getStagingClient()).deleteAllReports();
	}

	void deleteReport(HubClient hubClient, String uri) {
		hubClient.getStagingClient().newJSONDocumentManager().delete(uri);
	}

	private class GetUrisTask implements Runnable {
		private final DatabaseClient databaseClient;
		private final String collection;
		private final long start;
		private final long pageLength;
		private final long limit;
		private final int batchSize;
		private final Consumer<List<String>> urisIterator;

		GetUrisTask(DatabaseClient databaseClient, String collection, long start, long pageLength, long limit, int batchSize, Consumer<List<String>> urisIterator)  {
			this.databaseClient = databaseClient;
			this.collection = collection;
			this.start = start;
			this.pageLength = pageLength;
			this.limit = limit;
			this.batchSize = batchSize;
			this.urisIterator = urisIterator;
		}

		public void run() {
			long end = Math.min(limit, start + pageLength - 1);
			String query = String.format("(cts:search(fn:collection(\"%s\"), cts:true-query(), (\"unfiltered\", \"score-random\")))[%d to %d] ! xdmp:node-uri(.)", collection, start, end);
			EvalResultIterator resultIterator = databaseClient.newServerEval()
				.xquery(query)
				.eval();
			List<String> uris = new ArrayList<>();
			while(resultIterator.hasNext()) {
				uris.add(resultIterator.next().getString());
			}

			if ( uris.size() == batchSize ) {
				long nextStart = start + pageLength;
				if (nextStart <= limit) {
					// this is a full batch
					threadPool.execute(new GetUrisTask(databaseClient, collection, nextStart, pageLength, limit, batchSize, urisIterator));
				}
				else {
					threadPool.shutdown();
				}
			}
			else {
				threadPool.shutdown();
			}

			urisIterator.accept(uris);
		}
	}
}
