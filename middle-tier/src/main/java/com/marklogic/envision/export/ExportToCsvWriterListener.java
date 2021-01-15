package com.marklogic.envision.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.datamovement.*;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.JacksonHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExportToCsvWriterListener extends ExportListener {
	private static Logger logger = LoggerFactory.getLogger(ExportToCsvWriterListener.class);
	private Writer writer;
	private String entityName;
	private JsonNode model;
	private AtomicBoolean isFirstRow = new AtomicBoolean(true);
	private ObjectMapper objectMapper = new ObjectMapper();

	public ExportToCsvWriterListener(Writer writer, JsonNode model) {
		this.writer = writer;
		this.model = model;
		this.withTransform(new ServerTransform("redactDoc"));
		logger.debug("new ExportToWriterListener - this should print once/job; " +
			"if you see this once/batch, fix your job configuration");
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	private String[] getHeaders(String entityName) {
		JsonNode entity = this.model.get("nodes").get(entityName.toLowerCase());
		ArrayNode properties = (ArrayNode)entity.get("properties");
		String[] headers = new String[properties.size()];
		for (int i = 0; i < properties.size(); i++) {
			headers[i] = properties.get(i).get("name").asText();
		}
		return headers;
	}

	@Override
	public void processEvent(QueryBatch batch) {
		try {
			DocumentPage docs = getDocs(batch);
			synchronized(writer) {
				String[] headers = getHeaders(this.entityName);

				if (isFirstRow.get()) {
					writer.write(String.join(",", headers) + "\n");
					isFirstRow.set(false);
				}

				for ( DocumentRecord doc : docs ) {
					Format format = doc.getFormat();
					if ( Format.BINARY.equals(format) ) {
						throw new IllegalStateException("Document " + doc.getUri() +
							" is binary and cannot be written.  Change your query to not select any binary documents.");
					} else {
						try {
							writer.write(toCSV(doc, headers));
						} catch (IOException e) {
							throw new DataMovementException("Failed to write document \"" + doc.getUri() + "\"", e);
						}
					}
				}
			}
		}
		catch (Throwable t) {
			for ( BatchFailureListener<Batch<String>> listener : getFailureListeners() ) {
				try {
					listener.processFailure(batch, t);
				} catch (Throwable t2) {
					logger.error("Exception thrown by an onBatchFailure listener", t2);
				}
			}
			for ( BatchFailureListener<QueryBatch> queryBatchFailureListener : getBatchFailureListeners() ) {
				try {
					queryBatchFailureListener.processFailure(batch, t);
				} catch (Throwable t2) {
					logger.error("Exception thrown by an onFailure listener", t2);
				}
			}
		}
	}

	private String toCSV(DocumentRecord documentRecord, String[] headers) {
		if (Format.XML.equals(documentRecord.getFormat())) {
			DOMHandle handle = documentRecord.getContent(new DOMHandle());
			Document document = handle.get();
			Element instance = (Element)document.getDocumentElement().getElementsByTagNameNS("*", "instance").item(0);
			NodeList entityList = instance.getElementsByTagName(entityName);
			Element entity = instance;
			if (entityList.getLength() == 1) {
				Node e = entityList.item(0);
				if (e instanceof Element) {
					entity = (Element)e;
				}
			}
			List<String> values = new ArrayList<>();
			for (int i = 0; i < headers.length; i++) {
				NodeList children = entity.getElementsByTagName(headers[i]);
				if (children.getLength() == 1) {
					Element child = (Element)children.item(0);
					if (child != null) {
						String value = child.getTextContent();
						if (value != null) {
							if (value.contains(",")) {
								value = "\"" + value + "\"";
							}
							values.add(value);
						}
						else {
							values.add("");
						}
					}
					else {
						values.add("");
					}
				}
				else {
					values.add("");
				}
			}

			return String.join(",", values) + "\n";
		}
		else if (Format.JSON.equals(documentRecord.getFormat())) {
			JacksonHandle handle = documentRecord.getContent(new JacksonHandle());
			ObjectNode node = (ObjectNode)handle.get();
			ObjectNode instance = (ObjectNode)node.get("envelope").get("instance");
			ObjectNode entity = (ObjectNode)instance.get(entityName);
			List<String> values = new ArrayList<>();
			for (int i = 0; i < headers.length; i++) {
				JsonNode childNode = entity.get(headers[i]);
				if (childNode != null) {
					String value = null;
					if (childNode.isArray()) {
						try {
							value = objectMapper.writeValueAsString(childNode);
						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
					}
					else {
						value = childNode.asText();
					}
					if (value.contains(",") || value.contains("\"") || value.contains("'")) {
						value = value.replaceAll("\"", "\"\"");
						value = "\"" + value + "\"";
					}
					values.add(value);
				}
				else {
					values.add("");
				}
			}
			return String.join(",", values) + "\n";
		}
		else if (logger.isDebugEnabled()) {
			logger.debug(String.format("Document '%s' has a format of '%s', so will not attempt to remove the XML declaration from it",
				documentRecord.getUri(), documentRecord.getFormat().name()));
		}

		return "";
	}
}
