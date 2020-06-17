package com.marklogic.envision.mastering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.GenericDocumentManager;
import com.marklogic.client.extensions.ResourceManager;
import com.marklogic.client.extensions.ResourceServices;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.marker.AbstractWriteHandle;
import com.marklogic.client.util.RequestParameters;
import com.marklogic.envision.dataServices.Mastering;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
public class MasteringService {

	private final String MASTERING_STATS = "mastering-stats";
	private final String MASTERING_MATCH = "mlSmMatch";
	private final String MASTERING_MERGE = "mlSmMerge";
	private final String MASTERING_HISTORY_DOCUMENT = "mlSmHistoryDocument";
	private final String MASTERING_HISTORY_PROPERTIES = "mlSmHistoryProperties";
	private final String MASTERING_BLOCK_MATCH = "sm-block-match";
	private final String SM_NOTIFICATIONS = "mlSmNotifications";

	public String getStats(DatabaseClient client) {
		return new GenericResourceManager(MASTERING_STATS, client).get();
	}

	public String getDoc(DatabaseClient client, String docUri) {
		GenericDocumentManager docMgr = client.newDocumentManager();
		return docMgr.readAs(docUri, String.class);
	}

	public JsonNode mergeDocs(DatabaseClient client, ArrayNode uris, String flowName, String stepNumber, Boolean preview) throws IOException {
		return Mastering.on(client).merge(uris, flowName, stepNumber, preview, UUID.randomUUID().toString(), null);
	}

	public JsonNode unmerge(DatabaseClient client, String uri) {
		return Mastering.on(client).unmerge(uri);
	}

	public String getHistoryDocument(DatabaseClient client, String uri) {
		RequestParameters params = new RequestParameters();
		params.add("uri", uri);
		return new GenericResourceManager(MASTERING_HISTORY_DOCUMENT, client).get(params);
	}

	public String getHistoryProperties(DatabaseClient client, String uri) {
		RequestParameters params = new RequestParameters();
		params.add("uri", uri);
		return new GenericResourceManager(MASTERING_HISTORY_PROPERTIES, client).get(params);
	}

	public String getBlockedMatches(DatabaseClient client, String uri) {
		RequestParameters params = new RequestParameters();
		params.add("uri", uri);
		return new GenericResourceManager(MASTERING_BLOCK_MATCH, client).get(params);
	}

	public void blockMatch(DatabaseClient client, String uri1, String uri2) {
		RequestParameters params = new RequestParameters();
		params.add("uri1", uri1);
		params.add("uri2", uri2);
		new GenericResourceManager(MASTERING_BLOCK_MATCH, client).post(params, new StringHandle("").withFormat(Format.JSON));
	}

	public void unblockMatch(DatabaseClient client, String uri1, String uri2) {
		RequestParameters params = new RequestParameters();
		params.add("uri1", uri1);
		params.add("uri2", uri2);
		new GenericResourceManager(MASTERING_BLOCK_MATCH, client).delete(params);
	}

	public JsonNode getNotification(DatabaseClient client, String uri) {
		return Mastering.on(client).getNotification(uri);
	}

	public JsonNode getNotifications(DatabaseClient client, String qtext, Integer page, Integer pageLength, String sort) {
		return Mastering.on(client).getNotifications(qtext, page, pageLength, sort);
	}

	public JsonNode updateNotifications(DatabaseClient client, ArrayNode uris, String status) {
		return Mastering.on(client).updateNotifications(uris, status);
	}

	public JsonNode getBlocks(DatabaseClient client, ArrayNode uris) {
		return Mastering.on(client).getBlocks(uris);
	}

	public JsonNode block(DatabaseClient client, ArrayNode uris) {
		return Mastering.on(client).block(uris);
	}

	public JsonNode unblock(DatabaseClient client, ArrayNode uris) {
		return Mastering.on(client).unBlock(uris);
	}

	public void deleteNotifications(DatabaseClient client, String uri) {
		RequestParameters params = new RequestParameters();
		params.add("uri", uri);
		new GenericResourceManager(SM_NOTIFICATIONS, client).delete(params);
	}

	class GenericResourceManager extends ResourceManager {
		public GenericResourceManager(String name, DatabaseClient client) {
			super();
			client.init(name, this);
		}

		public String get() {
			return get(new RequestParameters());
		}

		public String get(RequestParameters params) {
			try {
				ResourceServices.ServiceResultIterator resultItr = this.getServices().get(params);
				if (resultItr == null || ! resultItr.hasNext()) { return "{}"; }
				ResourceServices.ServiceResult res = resultItr.next();
				StringHandle handle = new StringHandle();
				return res.getContent(handle).get();
			}
			catch(Exception e) {
			}
			return "{}";
		}

		public String put(RequestParameters params, AbstractWriteHandle input) {
			try {
				return this.getServices().put(params, input, new StringHandle()).get();
			}
			catch(Exception e) {
			}
			return "{}";
		}

		public void delete(RequestParameters params) {
			try {
				StringHandle handle = new StringHandle();
				this.getServices().delete(params, handle);
			}
			catch(Exception e) {
			}
		}

		public String post(RequestParameters params, AbstractWriteHandle input) {
			try {
				ResourceServices.ServiceResultIterator resultItr = this.getServices().post(params, input);
				if (resultItr == null || ! resultItr.hasNext()) { return "{}"; }
				ResourceServices.ServiceResult res = resultItr.next();
				StringHandle handle = new StringHandle();
				return res.getContent(handle).get();
			}
			catch(Exception e) {
			}
			return "{}";
		}
	}
}
