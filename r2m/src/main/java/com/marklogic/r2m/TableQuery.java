package com.marklogic.r2m;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TableQuery {
	private String query;
	private List<String> primaryKeyColumnNames = new ArrayList<>();
	private String propertyName;

	// Optional - for child table
	private List<String> foreignKeyColumnNames = new ArrayList<>();
	private List<String> foreignKeyQuotedValues = new ArrayList<>();
	private Set<String> foreignKeyQuotedValuesSet = new HashSet<>();
	private boolean isSetInitialized = false;
	
	private List<TableQuery> childQueries = new ArrayList<>();

	private String primaryKeyColumnsKey = null;
	private String foreignKeyColumnsKey = null;
	
	public TableQuery() {
		// Needed for JSON deserialization
	}

	public TableQuery(String query, List<String> primaryKeyColumnNames, List<String> foreignKeyColumnNames, List<String> foreignKeyQuotedValues, String propertyName) {
		this.query = query;
		this.primaryKeyColumnNames = primaryKeyColumnNames;
		this.foreignKeyColumnNames = foreignKeyColumnNames;
		this.foreignKeyQuotedValues = foreignKeyQuotedValues;
		this.propertyName = propertyName;
	}

	public void addChildQuery(TableQuery tableQuery) {
		this.childQueries.add(tableQuery);
	}

	public String getQuery() {
		return query;
	}

	public List<String> getPrimaryKeyColumnNames() {
		return primaryKeyColumnNames;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public List<String> getForeignKeyColumnNames() {
		return foreignKeyColumnNames;
	}
	
	public List<String> getForeignKeyQuotedValues() {
		return foreignKeyQuotedValues;
	}

	public List<TableQuery> getChildQueries() {
		return childQueries;
	}
	
	public String getPrimaryKeyColumnsKey() {
		if (foreignKeyColumnsKey == null) {
			foreignKeyColumnsKey = String.join("^", primaryKeyColumnNames);
		}
		return foreignKeyColumnsKey;
	}
	
	public String getForeignKeyColumnsKey() {
		if (primaryKeyColumnsKey == null) {
			primaryKeyColumnsKey = String.join("^", foreignKeyColumnNames);
		}
		return primaryKeyColumnsKey;
	}
	
	public boolean isForeignKeyValueQuoted(String key) {
		// init set if not already
		if (!isSetInitialized) {
			foreignKeyQuotedValues.forEach(column -> foreignKeyQuotedValuesSet.add(column));
			isSetInitialized = true;
		}
		
		return foreignKeyQuotedValuesSet.contains(key);
	}
}
