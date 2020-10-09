package com.marklogic.envision.export;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.datamovement.*;
import com.marklogic.client.document.DocumentManager;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;
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

public class ExportToCsvWriterListener extends ExportListener {
	private static Logger logger = LoggerFactory.getLogger(com.marklogic.client.datamovement.ExportToWriterListener.class);
	private Writer writer;
	private boolean isFirstRow = true;
	private List<com.marklogic.client.datamovement.ExportToWriterListener.OutputListener> outputListeners = new ArrayList<>();

	public ExportToCsvWriterListener(Writer writer) {
		this.writer = writer;
		logger.debug("new ExportToWriterListener - this should print once/job; " +
			"if you see this once/batch, fix your job configuration");
	}

	/**
	 * This implementation of initializeListener adds this instance of
	 * ExportToWriterListener to the two RetryListener's in this QueryBatcher so they
	 * will retry any batches that fail during the read request.
	 */
	@Override
	public void initializeListener(QueryBatcher queryBatcher) {
		HostAvailabilityListener hostAvailabilityListener = HostAvailabilityListener.getInstance(queryBatcher);
		if ( hostAvailabilityListener != null ) {
			BatchFailureListener<QueryBatch> retryListener = hostAvailabilityListener.initializeRetryListener(this);
			if ( retryListener != null )  onFailure(retryListener);
		}
		NoResponseListener noResponseListener = NoResponseListener.getInstance(queryBatcher);
		if ( noResponseListener != null ) {
			BatchFailureListener<QueryBatch> noResponseRetryListener = noResponseListener.initializeRetryListener(this);
			if ( noResponseRetryListener != null )  onFailure(noResponseRetryListener);
		}
	}

	@Override
	public void processEvent(QueryBatch batch) {
		try ( DocumentPage docs = getDocs(batch) ) {
			synchronized(writer) {
				for ( DocumentRecord doc : docs ) {
					Format format = doc.getFormat();
					if ( Format.BINARY.equals(format) ) {
						throw new IllegalStateException("Document " + doc.getUri() +
							" is binary and cannot be written.  Change your query to not select any binary documents.");
					} else {
						try {
							writer.write(toCSV(doc, isFirstRow));
							isFirstRow = false;
						} catch (IOException e) {
							throw new DataMovementException("Failed to write document \"" + doc.getUri() + "\"", e);
						}
					}
				}
			}
		} catch (Throwable t) {
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

	/**
	 * Registers a custom listener to override the default behavior for each
	 * document which sends the document contents to the writer.  This listener
	 * can choose what string to send to the writer for each document.
	 *
	 * @param listener the custom listener (or lambda expression)
	 * @return this instance (for method chaining)
	 */
	public ExportToCsvWriterListener onGenerateOutput(com.marklogic.client.datamovement.ExportToWriterListener.OutputListener listener) {
		outputListeners.add(listener);
		return this;
	}

	/**
	 * The listener interface required by onGenerateOutput.
	 */
	public static interface OutputListener {
		/**
		 * Given the DocumentRecord, generate the desired String output to send to the writer.
		 *
		 * @param record the document retrieved from the server
		 * @return the String output to send to the writer
		 */
		public String generateOutput(DocumentRecord record);
	}

	// override the following just to narrow the return type
	@Override
	public ExportToCsvWriterListener withTransform(ServerTransform transform) {
		super.withTransform(transform);
		return this;
	}

  /* TODO: test to see if QueryView is really necessary
  @Override
  public ExportToWriterListener withSearchView(QueryManager.QueryView view) {
    super.withSearchView(view);
    return this;
  }
  */

	/**
	 * Adds a metadata category to retrieve with each document.  The metadata
	 * will be available via {@link DocumentRecord#getMetadata
	 * DocumentRecord.getMetadata} in each DocumentRecord sent to the
	 * OutputListener registered with onGenerateOutput.  To specify the format
	 * for the metdata, call {@link #withNonDocumentFormat
	 * withNonDocumentFormat}.
	 *
	 * @param category the metadata category to retrieve
	 * @return this instance (for method chaining)
	 */
	@Override
	public ExportToCsvWriterListener withMetadataCategory(DocumentManager.Metadata category) {
		super.withMetadataCategory(category);
		return this;
	}

	/**
	 * The format for the metadata retrieved with each document.  The metadata will
	 * be available in each DocumentRecord sent to the OutputListener registered
	 * with onGenerateOutput.
	 *
	 * @param nonDocumentFormat the format for the metadata
	 * @return this instance (for method chaining)
	 */
	@Override
	public ExportToCsvWriterListener withNonDocumentFormat(Format nonDocumentFormat) {
		super.withNonDocumentFormat(nonDocumentFormat);
		return this;
	}

}
