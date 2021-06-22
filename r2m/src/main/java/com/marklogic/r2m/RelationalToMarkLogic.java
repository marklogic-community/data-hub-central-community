package com.marklogic.r2m;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.jdbc.core.ColumnMapRowMapper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RelationalToMarkLogic {

	public RelationalToMarkLogic() {
		objectMapper = new ObjectMapper();

		if (sqlDateFormat != null) {
			// TODO : Make this configurable
			System.out.println("Using format for serializing instances of java.sql.Date: " + sqlDateFormat);
			SimpleModule simpleModule = new SimpleModule();
			simpleModule.addSerializer(Date.class, new SqlDateSerializer(sqlDateFormat));
			objectMapper.registerModule(simpleModule);
		}
	}

	private Connection connection;

	// FSnow: pull from config, was NiFi processor property
	private static final String sqlDateFormat = "yyyy-MM-dd";

	// Three classes representing the JSON configuration files
	private TableQuery tableQuery;
	private MarkLogicConfiguration marklogicConfiguration;
	private SourceConfiguration sourceConfig;
	private MLInsertConfig insertConfig;

	private String query;

	private BlockingQueue<String> docQueue;
	private BlockingQueue<List<Map<String, Object>>> rowQueue;
	private int numChildQueryExecutors;
	private int docQueueSize;
	private ChildQueryExecutor childQueryExecutors[];
	private DocumentLoader docLoaders[];
	private int joinBatchSize = 100;

	private String exitFlag = "Done";

	private ObjectMapper objectMapper = new ObjectMapper();

	private String dbUser;

	public void setDBPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}
	private String dbPassword;

	private void init() throws Exception {
		if (this.tableQuery == null) {
			throw new Exception("No join configuration present");
		}

		if (this.sourceConfig == null) {
			throw new Exception("No source configuration present");
		}

		if (this.marklogicConfiguration == null) {
			throw new Exception("No marklogic configuration present");
		}

		if (this.insertConfig == null) {
			throw new Exception("No insert configuration present");
		}

		connection = DriverManager.getConnection(sourceConfig.getConnectionString(), sourceConfig.getUsername(), sourceConfig.getPassword());
		numChildQueryExecutors = sourceConfig.getNumThreads();
		childQueryExecutors = new ChildQueryExecutor[numChildQueryExecutors];
		joinBatchSize = sourceConfig.getBatchSize();

		query = tableQuery.getQuery();

		// Set the document queue to be large enough such that every document loader can grab a full batch should they all attempt
		// to pull from the queue at the same time
		docQueueSize = marklogicConfiguration.getBatchSize() * marklogicConfiguration.getNumThreadsPerHost() * marklogicConfiguration.getHosts().size();
		docQueue = new LinkedBlockingQueue<>(docQueueSize);

		// Set the row queue to be large enough such that every child query executor can grab a job should they all attempt to pull
		// from the queue at the same time
		rowQueue = new LinkedBlockingQueue<>(numChildQueryExecutors);
	}

	public void run() throws Exception {

		init();

		final ColumnMapRowMapper rowMapper = new ColumnMapRowMapper();

		createDocumentLoaders();
		createChildQueryExecutors();

		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int rowNumber = 0;
		int totalRows = 0;
		int printFrequency = numChildQueryExecutors * joinBatchSize;
		try {
			System.out.println("Executing query: " + query);

			try {
				preparedStatement = connection.prepareStatement(query);
				resultSet = preparedStatement.executeQuery();
			} catch (SQLException e) {
				System.out.println("Error in Executing Query "+query+" " +e.toString());
			};


			List<Map<String, Object>> columnMaps = new ArrayList<>();
			while (resultSet.next()) {
				columnMaps.add(rowMapper.mapRow(resultSet, rowNumber));

				rowNumber++;
				if (rowNumber >= joinBatchSize) {
					executeChildQueries(columnMaps);
					totalRows += rowNumber;
					if(totalRows % printFrequency == 0) {
						System.out.print("\rSent " + totalRows + " rows for processing.  Loader queue capacity: " + docQueue.remainingCapacity());
					}
					rowNumber = 0;
					columnMaps = new ArrayList<>();
				}
			}

			totalRows += rowNumber;

			// ResultSet is complete, so process final batch if it exists
			// Wait for the queue to empty and then terminate the child threads
			// Send
			if (!columnMaps.isEmpty()) {
				System.out.println("Sending final batch of size: " + columnMaps.size());
				executeChildQueries(columnMaps);
			}
		} catch (SQLException ex) {
			throw ex;
		} finally {
			if (resultSet != null) {
				try {
					resultSet.close();
				} catch (SQLException e) {
					// ignore
				}
			}
			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					// ignore
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// ignore
				}
			}
		}
		// Watch both queues to see when they finally empty.
		System.out.println("Placed " + totalRows + " documents in queue to be loaded");
		System.out.println("Shutting down child query executors...");

		int childQueriesRemaining = 0;
		int remainingJobs = numChildQueryExecutors;
		do {
			childQueriesRemaining = rowQueue.remainingCapacity();
			if(childQueriesRemaining == numChildQueryExecutors) {
				System.out.println("Child query queue exhausted.  Sending signal to child query executors to shut down");
				for(int i = 0; i < childQueryExecutors.length; i++) {
					childQueryExecutors[i].stopRunning();
				}
			} else {
				if(remainingJobs > numChildQueryExecutors - childQueriesRemaining) {
					remainingJobs = numChildQueryExecutors - childQueriesRemaining;
					System.out.print("\r" + remainingJobs + " child executors still processing");
				}
				Thread.sleep(1000);
			}
		} while (childQueriesRemaining < numChildQueryExecutors);

		for(int i = 0; i < childQueryExecutors.length; i++) {
			System.out.print("\rWaiting for executor #" + i + " to shut down");
			while(!childQueryExecutors[i].doneRunning) {
				Thread.sleep(1000);
			}
		}

		System.out.println("Shutting down document loaders...");
		int docsRemaining = 0;
		do {
			docsRemaining = docQueue.remainingCapacity();
			if(docsRemaining == docQueueSize) {
				System.out.println("Document queue exhausted.  Sending signal to document loaders to shut down");
				for(int i = 0; i < docLoaders.length; i++) {
					docLoaders[i].stopRunning();
				}
			} else {
				int remainingDocs = docQueueSize - docsRemaining;
				System.out.println(remainingDocs + " documents to be loaded");
				Thread.sleep(1000);
			}
		} while (docsRemaining < docQueueSize);

		System.out.println("Done!");
	}

	protected void executeChildQueries(List<Map<String, Object>> columnMapList) {
		try {
			rowQueue.put(columnMapList);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void createChildQueryExecutors() throws SQLException {
		for(int i = 0; i < childQueryExecutors.length; i++) {
			ChildQueryExecutor cqe = new ChildQueryExecutor(sourceConfig, tableQuery, insertConfig.getDocType(), rowQueue, docQueue);
			childQueryExecutors[i] = cqe;
			new Thread(cqe).start();
		}
	}

	private void createDocumentLoaders() throws JsonProcessingException {
		List<String> hosts = marklogicConfiguration.getHosts();
		int i = 0;
		String insertConfigJson = this.objectMapper.writeValueAsString(insertConfig);
		docLoaders = new DocumentLoader[hosts.size() * marklogicConfiguration.getNumThreadsPerHost()];
		for (String host : hosts) {
			for(int j = 0; j < marklogicConfiguration.getNumThreadsPerHost(); j++) {
				DocumentLoader loader = new DocumentLoader(docQueue, insertConfigJson, host, marklogicConfiguration);
				docLoaders[i] = loader;
				new Thread(loader).start();
				i++;
			}
		}
	}

	public TableQuery getTableQuery() {
		return tableQuery;
	}

	public void setTableQuery(TableQuery tableQuery) {
		this.tableQuery = tableQuery;
	}

	public MarkLogicConfiguration getMarklogicConfiguration() {
		return marklogicConfiguration;
	}

	public void setMarklogicConfiguration(MarkLogicConfiguration marklogicConfiguration) {
		this.marklogicConfiguration = marklogicConfiguration;
	}

	public SourceConfiguration getSourceConfig() {
		return sourceConfig;
	}

	public void setSourceConfig(SourceConfiguration sourceConfig) {
		this.sourceConfig = sourceConfig;
	}

	public MLInsertConfig getInsertConfig() {
		return insertConfig;
	}

	public void setInsertConfig(MLInsertConfig insertConfig) {
		this.insertConfig = insertConfig;
	}
}

