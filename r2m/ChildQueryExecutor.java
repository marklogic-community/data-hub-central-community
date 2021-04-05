package com.marklogic.r2m;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;


public class ChildQueryExecutor implements Runnable {

	private final String KEY_DELIMITER = "^";
	private final String NO_JOIN_FLAG = "__**NOJOIN**__";
	private final int MAX_QUERY_LENGTH = 100000;
	private String dbConnectionString;
	private String dbUser;
	private String dbPassword;
	private String docType;
	private volatile boolean stopRunning = false;
	public volatile boolean doneRunning = false;
	// FSnow: pull from config, was NiFi processor property
	private static final String sqlDateFormat = "yyyy-MM-dd";
	
	private float longestQueryTime = 0;
	
	private ObjectMapper objectMapper;

	private RowMapper<Map<String, Object>> columnMapRowMapper;
	
	private Connection connection;
	private TableQuery parentTableQuery;
	private SourceConfiguration sourceConfig;
	private BlockingQueue<String> docQueue;
	private BlockingQueue<List<Map<String, Object>>> rowQueue;

	public ChildQueryExecutor(SourceConfiguration sourceConfig, TableQuery parentTableQuery, String docType,
	BlockingQueue<List<Map<String, Object>>> rowQueue, BlockingQueue<String> docQueue) throws SQLException {
		this.sourceConfig = sourceConfig;
		this.columnMapRowMapper = new ColumnMapRowMapper();
		this.dbConnectionString = sourceConfig.getConnectionString();
		this.dbUser = sourceConfig.getUsername();
		this.dbPassword = sourceConfig.getPassword();
		this.docType = docType;
		this.connection = DriverManager.getConnection(dbConnectionString, dbUser, dbPassword);
		this.parentTableQuery = parentTableQuery;
		this.rowQueue = rowQueue;
		this.docQueue = docQueue;
		
		// Our object mapper is instantiated differently based on whether we'll be serializing XML or JSON
		// Default to JSON
		if("xml".equals(docType.toLowerCase())) {
			JacksonXmlModule xmlModule = new JacksonXmlModule();
			xmlModule.setDefaultUseWrapper(false);
			objectMapper = new XmlMapper(xmlModule);
		} else {
			objectMapper = new ObjectMapper().configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
			SimpleModule simpleModule = new SimpleModule();
			simpleModule.addSerializer(Date.class, new SqlDateSerializer(sqlDateFormat));
			simpleModule.addSerializer(Timestamp.class, new SqlTimestampSerializer());
			objectMapper.registerModule(simpleModule);
		}
	}
	
	@Override
    public void run() {
		List<Map<String, Object>> parentRows;
        try {
            do {
            	parentRows = rowQueue.poll();
				if(null != parentRows) {
					executeChildQueries(parentTableQuery, parentRows);
					writeColumnMaps(parentRows);
				} else {
					Thread.sleep(100);
				}
            } while (!stopRunning);
			connection.close();
        } catch (Exception e) {
			e.printStackTrace();
        }
		doneRunning = true;
    }
	
	public void stopRunning() {
		stopRunning = true;
	}

	/**
	 * @param connection
	 * @param parentTableQuery
	 * @param parentRows
	 */
	public void executeChildQueries(TableQuery parentTableQuery, List<? extends Map<String, Object>> parentRows) 
	{
		// For all the child queries of this parentTableQuery, get all the unique combinations of primary keys -- i.e. columns in
		// the parent -- that will be used to join to the child's foreign keys.
		Map<String, List<String>> childPrimaryKeyCombos = new LinkedHashMap<>();

		for (TableQuery childTableQuery : parentTableQuery.getChildQueries()) {
			String key = childTableQuery.getPrimaryKeyColumnsKey();
			if (childPrimaryKeyCombos.containsKey(key) == false) {
				childPrimaryKeyCombos.put(key, childTableQuery.getPrimaryKeyColumnNames());
			}
		}
		// Construct all the lookup maps over the parent rows.
		// The outer map will have a key that is the getPrimaryColumnsKey(), the caret-separated string of primary key names
		// The value of the outer map is a map with keys of concatenated primary key values and a value of a list of column maps,
		// with column name-value pairs
		Map<String, Map<String, List<Map<String, Object>>>> parentLookupMap = new LinkedHashMap<>();
		for (String primaryKeysKey : childPrimaryKeyCombos.keySet()) {
			List<String> primaryKeyCombo = childPrimaryKeyCombos.get(primaryKeysKey);
			
			// Construct a map based on primary keys so we can easily get the primary keys and populate the maps with kids later
			Map<String, List<Map<String, Object>>> primaryValuesMap = new LinkedHashMap<>();
			
			parentLookupMap.put(primaryKeysKey, primaryValuesMap);
			
			for (Map<String, Object> parentRow : parentRows) {
				String primaryValuesString = joinColumnValues(parentRow, primaryKeyCombo);

				if (primaryValuesString.equals(NO_JOIN_FLAG)) {
					// sparse data, do nothing.
				}
				else if (primaryValuesMap.containsKey(primaryValuesString)) {
					primaryValuesMap.get(primaryValuesString).add(parentRow);
				}
				else {
					List<Map<String, Object>> columnMapList = new ArrayList<>();
					columnMapList.add(parentRow);
					primaryValuesMap.put(primaryValuesString, columnMapList);
				}
			}
		}
		for (TableQuery childTableQuery : parentTableQuery.getChildQueries()) {			
			// we look up in the parent map by the child's primary key columns key, which represents the keys in the parent
			String primaryKeyColumnsKey = childTableQuery.getPrimaryKeyColumnsKey();
			
			// get the lookup map for this set of foreign keys
			Map<String, List<Map<String, Object>>> primaryValuesMap = parentLookupMap.get(primaryKeyColumnsKey);
			
			// if we have no primary values, there's nothing to do here.  Continue on to the next child table 
			if(primaryValuesMap.size() == 0) {
				continue;
			}
			
			// This is provided by user; can contain a where clause
			List<String> childQueries = constructQuery(childTableQuery, parentLookupMap);

			List<Map<String, Object>> childRows = new ArrayList<Map<String, Object>>();
			for(String childQuery : childQueries) {
				childRows.addAll(executeChildQuery(connection, childQuery));
			}
			
			
			// Now add each child map to the correct parent map
			// Note that for one-many relationships, there's no column in the parent object
			// TODO many-to-one are different, there is a column that we may want to replace, but can always transform it away
			boolean goDeeper = false;
			for (Map<String, Object> childRow : childRows) {
				
				// get the concatenated values string of child values to lookup in parent map
				String foreignValuesString = joinColumnValues(childRow, childTableQuery.getForeignKeyColumnNames());

				if (foreignValuesString.equals(NO_JOIN_FLAG)) {
					// sparse data, do nothing
				}
				else {
					// get the list of matching parent rows for this values string
					goDeeper = true;
					List<Map<String, Object>> matchingParentRows = primaryValuesMap.get(foreignValuesString);

					for (Map<String, Object> parentRow : matchingParentRows) {
						List<Map<String, Object>> kids;
						final String childElementName = childTableQuery.getPropertyName();
						if (parentRow.containsKey(childElementName)) {
							kids = (List<Map<String, Object>>) parentRow.get(childElementName);
						} else {
							kids = new ArrayList<>();
							parentRow.put(childElementName, kids);
						}
						kids.add(childRow);
					}
				}
			}
			// We only want to keep recursing if any children have been found
			if(childRows.size() > 0 && goDeeper) {
				executeChildQueries(childTableQuery, childRows);
			}
		}
	}
	
	protected void writeColumnMaps(List<Map<String, Object>> columnMapList) throws Exception {
		if("xml".equals(docType.toLowerCase())) {
			for (Map<String, Object> columnMap : columnMapList) {
				// serialize columpMap as XML document string
				// TODO: add root namespace to serialization... somehow
				String doc = objectMapper.writer().withRootName(parentTableQuery.getPropertyName()).writeValueAsString(columnMap);
				// write document string to queue
				docQueue.put(doc);
			}
		} else {
			for (Map<String, Object> columnMap : columnMapList) {
				try {
					// serialize columpMap as JSON document string
					String doc = objectMapper.writer().withRootName(parentTableQuery.getPropertyName()).writeValueAsString(columnMap);
					// write document string to queue
					docQueue.put(doc);
				} catch (JsonProcessingException e) {
					throw new Exception("Unable to write column map to JSON, cause: " + e.getMessage(), e);
				}
			}
		}
	}
	
	private String joinColumnValues(Map<String, Object> columnMap, List<String> columnNames) {
		List<String> columnValues = new ArrayList<>();
		boolean hasEmptyVals = false;
		for (String name : columnNames) {
			Object val = columnMap.get(name);
			if (val == null) {
				hasEmptyVals = true;
			}
			else {
				columnValues.add(val.toString());
			}
		}
		if (hasEmptyVals) {
			return NO_JOIN_FLAG;
		}
		else {
			return String.join(KEY_DELIMITER, columnValues);
		}
	}
	
	protected List<String> constructQuery(TableQuery childTableQuery, Map<String, Map<String, List<Map<String, Object>>>> parentLookupMap) {
		List<String> childForeignKeys = childTableQuery.getForeignKeyColumnNames();
		List<String> childPrimaryKeys = childTableQuery.getPrimaryKeyColumnNames();
		List<StringBuilder> additionalClauses = new ArrayList<StringBuilder>();
		
		if (childForeignKeys.size() == 1) {
			StringBuilder addlClause = new StringBuilder();
			// get the lookup map for this set of foreign keys
			String foreignKey = childForeignKeys.get(0);
			String primaryKey = childPrimaryKeys.get(0);
			Map<String, List<Map<String, Object>>> primaryValuesMap = parentLookupMap.get(primaryKey);
			
			boolean quoteValue = childTableQuery.isForeignKeyValueQuoted(foreignKey);
			addlClause.append(foreignKey).append(" IN (");
			
			boolean firstOne = true;
			for (String parentId : primaryValuesMap.keySet()) {
				if (!firstOne) {
					addlClause.append(",");
				}
				if(quoteValue) {
					addlClause.append("'" + parentId + "'");
				} else {
					addlClause.append(parentId);
				}
				firstOne = false;
			}
			addlClause.append(")");
			additionalClauses.add(addlClause);
		}
		else {
			// get the lookup map for this set of primary keys
			Map<String, List<Map<String, Object>>> primaryValuesMap = parentLookupMap.get(childTableQuery.getPrimaryKeyColumnsKey());
			
			additionalClauses = buildAdditionalClauses(childTableQuery, childPrimaryKeys, childForeignKeys, primaryValuesMap);
		}
		List<String> childQueries = new ArrayList<String>();
		for(StringBuilder addlClause : additionalClauses) {
			// childQuery is provided by the user and can contain a WHERE clause
			String childQuery = childTableQuery.getQuery();
			String lowerCaseQuery = childQuery.toLowerCase();
			if (!lowerCaseQuery.contains(" where ")) {
				childQuery += " WHERE ";
			} else {
				childQuery += " AND ";
			}
			childQuery += addlClause;
			childQueries.add(childQuery);
		}
		return childQueries;
	}
	
	protected List<StringBuilder> buildAdditionalClauses(TableQuery childTableQuery, List<String> childPrimaryKeys, List<String> childForeignKeys, 
	Map<String, List<Map<String, Object>>> primaryValuesMap) {
		// ((col1 = col1val1 AND col2 = col2val1 AND col3 = col3val1) OR
		// (col1 = col1val2 AND col2 = col2val2 AND col3 = col3val2) OR
		// (col1 = col1val3 AND col2 = col2val3 AND col3 = col3val3))
		
		// iterate over primaryValuesMap keys
		// get the first column map from the primaryValuesMap value list 
		// (each one in that list has the same values for the primary keys)
		// get each of the primary key values from that column map and build one AND'ed expression of the WHERE clause
		StringBuilder addlClause = new StringBuilder();
		addlClause.append("(");
			
		int i = 0;
		
		for (String primaryValuesKey : primaryValuesMap.keySet()) {
			if (i > 0) {
				addlClause.append(" OR ");
			}
			
			addlClause.append("(");
			
			Map<String, Object> columnMap = primaryValuesMap.get(primaryValuesKey).get(0);
			
			int j = 0;
			for (String childForeignKey : childForeignKeys) {
				
				if (j > 0) {
					addlClause.append(" AND ");
				}
				
				boolean isQuoted = childTableQuery.isForeignKeyValueQuoted(childForeignKey);
				addlClause.append(childForeignKey);
				addlClause.append(" = ");
				
				if (isQuoted) addlClause.append("'");
				String childValue = columnMap.get(childPrimaryKeys.get(j)).toString();
				if (isQuoted) {
					childValue = childValue.replaceAll("'", "''");
				}
				addlClause.append(childValue);
				if (isQuoted) addlClause.append("'");
				
				j++;
			}
			
			addlClause.append(")");
			
			i++;
		}
		addlClause.append(")");
		List<StringBuilder> returnList = new ArrayList<StringBuilder>();
		//Query is safe to execute- go ahead and return
		if(addlClause.length() < MAX_QUERY_LENGTH) {
			returnList.add(addlClause);
		} 
		//Query is too large- keep splitting in half until safe
		else {
			//Split the primary values key set and try again
			Map<String, List<Map<String, Object>>> smallerMapA = new LinkedHashMap<String, List<Map<String, Object>>>();
			Map<String, List<Map<String, Object>>> smallerMapB = new LinkedHashMap<String, List<Map<String, Object>>>();
			boolean b = true;
			for(Map.Entry<String, List<Map<String, Object>>> e : primaryValuesMap.entrySet()) {
				if(b) {
					smallerMapA.put(e.getKey(), e.getValue());
				}
				else {
					smallerMapB.put(e.getKey(), e.getValue());
				}
				b = !b;
			}
			returnList.addAll(buildAdditionalClauses(childTableQuery, childPrimaryKeys, childForeignKeys, smallerMapA));
			returnList.addAll(buildAdditionalClauses(childTableQuery, childPrimaryKeys, childForeignKeys, smallerMapB));
		}
		
		return returnList;
	}
	
	protected void printLongestQuery(float queryTime, String query) {
		if(queryTime > longestQueryTime) {
			longestQueryTime = queryTime;
			//Uncomment this to debug problem queries in complicated entities
			//System.out.println("New longest query took " + queryTime + " seconds: " + query);
		}
	}

	protected List<Map<String, Object>> executeChildQuery(Connection connection, String childQuery) {
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			preparedStatement = connection.prepareStatement(childQuery);
			long start = System.currentTimeMillis();
			resultSet = preparedStatement.executeQuery();
			long end = System.currentTimeMillis();
			float sec = (end - start) / 1000F;
			
			printLongestQuery(sec, childQuery);
			
			List<Map<String, Object>> childRows = new ArrayList<>();
			while (resultSet.next()) {
				childRows.add(columnMapRowMapper.mapRow(resultSet, 0));
			}
			return childRows;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
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
		}
	}

}
