package com.marklogic.envision.export;

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


	private AtomicBoolean isFirstRow = new AtomicBoolean(true);

	public ExportToCsvWriterListener(Writer writer) {
		this.writer = writer;
		this.withTransform(new ServerTransform("redactDoc"));
		logger.debug("new ExportToWriterListener - this should print once/job; " +
			"if you see this once/batch, fix your job configuration");
	}

	@Override
	public void processEvent(QueryBatch batch) {
		try {
			DocumentPage docs = getDocs(batch);
			synchronized(writer) {
				for ( DocumentRecord doc : docs ) {
					Format format = doc.getFormat();
					if ( Format.BINARY.equals(format) ) {
						throw new IllegalStateException("Document " + doc.getUri() +
							" is binary and cannot be written.  Change your query to not select any binary documents.");
					} else {
						try {
							writer.write(toCSV(doc, isFirstRow.get()));
							isFirstRow.set(false);
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

	private String toCSV(DocumentRecord documentRecord, boolean dumpHeaders) {
		if (Format.XML.equals(documentRecord.getFormat())) {
			DOMHandle handle = documentRecord.getContent(new DOMHandle());
			Document document = handle.get();
			Node instance = document.getDocumentElement().getElementsByTagNameNS("*", "instance").item(0);
			NodeList children = instance.getChildNodes();
			List<String> headers = new ArrayList<>();
			List<String> values = new ArrayList<>();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child != null && child.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element)child;
					headers.add(e.getLocalName());
					values.add(e.getNodeValue());
				}
			}

			String output = "";
			if (dumpHeaders) {
				output += String.join(",", headers) + "\n";
			}
			output += String.join(",", values) + "\n";
			return output;
		}
		else if (Format.JSON.equals(documentRecord.getFormat())) {
			JacksonHandle handle = documentRecord.getContent(new JacksonHandle());
			ObjectNode node = (ObjectNode)handle.get();
			ObjectNode instance = (ObjectNode)node.get("envelope").get("instance");
			List<String> headers = new ArrayList<>();
			List<String> values = new ArrayList<>();
			instance.fieldNames().forEachRemaining(s -> {
				if (!s.equals("info")) {
					ObjectNode entity = (ObjectNode)instance.get(s);
					entity.fields().forEachRemaining(stringJsonNodeEntry -> {
						headers.add(stringJsonNodeEntry.getKey());
						values.add(stringJsonNodeEntry.getValue().asText());
					});
				}
			});
			String output = "";
			if (dumpHeaders) {
				output += String.join(",", headers) + "\n";
			}
			output += String.join(",", values) + "\n";
			return output;
		}
		else if (logger.isDebugEnabled()) {
			logger.debug(String.format("Document '%s' has a format of '%s', so will not attempt to remove the XML declaration from it",
				documentRecord.getUri(), documentRecord.getFormat().name()));
		}

		return "";
	}
}
