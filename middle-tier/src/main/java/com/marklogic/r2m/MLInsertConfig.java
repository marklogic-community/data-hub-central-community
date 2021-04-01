package com.marklogic.r2m;

import java.util.ArrayList;
import java.util.List;

public class MLInsertConfig {
	private String entityName;
	private String entityNamespace;
	private String uriPrefix;
	private String uriSuffix;
	private List<String> keys = new ArrayList<>();
	private String keyDelimiter;
	private List<String> collections = new ArrayList<>();
	private String docType;
	
	public MLInsertConfig() {
		// Needed for JSON deserialization
	}
	
	public MLInsertConfig(String entityName, String entityNamespace, String uriPrefix, String uriSuffix, List<String> keys, String keyDelimiter, List<String> collections, String docType) {
		this.entityName = entityName;
		this.entityNamespace = entityNamespace;
		this.uriPrefix = uriPrefix;
		this.uriSuffix = uriSuffix;
		this.keys = keys;
		this.keyDelimiter = keyDelimiter;
		this.collections = collections;
		this.docType = docType;
	}
	
	public void addKey(String key) {
		    this.keys.add(key);
	}
	
	public void addCollection(String collection) {
		this.collections.add(collection);
	}
	
	public String getEntityName() {
		return this.entityName;
	}
	
	public String getEntityNamespace() {
		return this.entityNamespace;
	}
	
	public String getUriPrefix() {
		return this.uriPrefix;
	}
	
	public String getUriSuffix() {
		return this.uriSuffix;
	}
	
	public List<String> getKeys() {
		return this.keys;
	}
	
	public String getKeyDelimiter() {
		return this.keyDelimiter;
	}
	
	public List<String> getCollections() {
		return this.collections;
	}
	
	public String getDocType() {
		return this.docType;
	}
}
