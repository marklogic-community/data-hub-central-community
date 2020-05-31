package com.marklogic.dhf;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.datamovement.DataMovementManager;
import com.marklogic.client.datamovement.JobTicket;
import com.marklogic.client.datamovement.WriteBatcher;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.dhf.config.MarkLogicConfiguration;
import com.marklogic.dhf.config.ServerTransformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This service utilizes MarkLogic's data movement SDK to ingest content in batch.
 *
 * @author Drew Wanczowski
 */
@Service
public class DataMovementService {

    private final Logger logger = LoggerFactory.getLogger(DataMovementService.class);

    private final MarkLogicConfiguration markLogicConfiguration;

    private final DatabaseClient databaseClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public DataMovementService(MarkLogicConfiguration markLogicConfiguration, DatabaseClient databaseClient, ObjectMapper objectMapper) {
        this.markLogicConfiguration = markLogicConfiguration;
        this.databaseClient = databaseClient;
        this.objectMapper = objectMapper;
    }

    /**
     * A load process to convert and load POJOs into MarkLogic.
     *
     * @param jobId                - A identifier for the full load.
     * @param baseUri              - A URI prefix to add to the document.
     * @param additionalCollection - A collection tag to add to source objects.
     * @param objects              - List of objects to be converted to JSON
     * @return A list of commited URIs
     */
    public List<String> load(ServerTransformConfig serverTransformConfig, String jobId, String baseUri, String additionalCollection, List<?> objects) {
        try {

            final DataMovementManager dataMovementManager = this.databaseClient.newDataMovementManager();

            logger.info("Creating WriteBatcher wtih " + this.markLogicConfiguration.getThreads() + " threads and a batch size of " + this.markLogicConfiguration.getBatch());

            String collection = "dmsdk_" + jobId;

            List completedUris = Collections.synchronizedList(new ArrayList());

            /**
             * Prepare the write batcher for processing.
             */
            final WriteBatcher writeBatcher = dataMovementManager
                    .newWriteBatcher()
                    .withJobName("DMSDK Load " + jobId)
                    .withBatchSize(this.markLogicConfiguration.getBatch())
                    .withThreadCount(this.markLogicConfiguration.getThreads())
                    .onBatchSuccess((batch -> {
                        completedUris.addAll(
                                Arrays.stream(batch.getItems())
                                        .map(event -> event.getTargetUri())
                                        .collect(Collectors.toList()));
                    }))
                    .onBatchFailure((batch, throwable) -> {
                        throwable.printStackTrace();
                    });

            /**
             * Apply a server side transform if configured.
             */
            if (serverTransformConfig != null) {
                ServerTransform serverTransform = new ServerTransform(serverTransformConfig.getName());

                Map<String, String> params = serverTransformConfig.getParams();
                for (String key : params.keySet()) {
                    serverTransform.addParameter(key, params.get(key));
                }

                serverTransform.addParameter("job-id", jobId);
                writeBatcher.withTransform(serverTransform);
            }

            /**
             * Process the records.
             */
            final JobTicket jobTicket = dataMovementManager.startJob(writeBatcher);

            Set<String> mergedCollections = new HashSet<>();
            mergedCollections.add(collection);
            mergedCollections.addAll(this.markLogicConfiguration.getCollections());
            mergedCollections.add(additionalCollection);

            DocumentMetadataHandle documentMetadataHandle = new DocumentMetadataHandle()
                    .withCollections(mergedCollections.toArray(new String[0]));

            for (Object obj : objects) {
                writeBatcher.add(
                        baseUri + UUID.randomUUID().toString() + ".json",
                        documentMetadataHandle,
                        new StringHandle(this.objectMapper.writeValueAsString(obj)).withFormat(Format.JSON)
                );
            }

            writeBatcher.flushAndWait();
            dataMovementManager.stopJob(jobTicket);

            return completedUris;

        } catch (Exception e) {
            logger.error("Unable to load data", e);
        }

        return null;
    }
}