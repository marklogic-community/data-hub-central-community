package com.marklogic.r2m;

import java.util.ArrayList;
import java.util.List;

public class MarkLogicConfiguration {

	private List<String> hosts = new ArrayList<>();
	private int port;
	private int numThreadsPerHost;
	private int batchSize;
	private String username;
	private String password;
	private String authContext; // digest or basic

	public MarkLogicConfiguration() {
		// Needed for JSON deserialization
	}

	public MarkLogicConfiguration(List<String> hosts, int port, int numThreadsPerHost, int batchSize, 
			String username, String password, String authContext) 
	{
		this.hosts = hosts;
		this.port = port;
		this.numThreadsPerHost = numThreadsPerHost;
		this.batchSize = batchSize;
		this.username = username;
		this.password = password;
		this.authContext = authContext;
	}

	public void addHost(String host) {
		this.hosts.add(host);
	}

	public List<String> getHosts() {
		return hosts;
	}
	
	public int getPort() {
		return port;
	}

	public int getNumThreadsPerHost() {
		return numThreadsPerHost;
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

	public String getAuthContext() {
		return authContext;
	}
}
