package com.marklogic.r2m;

import java.util.ArrayList;
import java.util.List;

public class SourceConfiguration {

	private int numThreads;
	private int batchSize;
	private String username;
	private String password;
	private String connectionString; // h2, mysql, oracle

	public SourceConfiguration() {
		// Needed for JSON deserialization
	}

	public SourceConfiguration(int numThreads, int batchSize, String username, String password, String connectionString) 
	{
		this.numThreads = numThreads;
		this.batchSize = batchSize;
		this.username = username;
		this.password = password;
		this.connectionString = connectionString;
	}

	public int getNumThreads() {
		return numThreads;
	}

	public int getBatchSize() {
		return batchSize;
	}
	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getConnectionString() {
		return connectionString;
	}
}
