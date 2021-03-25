package com.marklogic.envision.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.pojo.PojoQueryDefinition;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import com.marklogic.envision.pojo.StatusMessage;
import com.marklogic.grove.boot.error.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportService {

	public static final String EXPORT_INFO_COLLECTION = "http://marklogic.com/envision/export-info";
	public static final String USER_EXPORT_COLLECTION = "http://marklogic.com/envision/export/";

	private final ObjectMapper objectMapper = new ObjectMapper();
	final private SimpMessagingTemplate template;
	final private ModelService modelService;

	@Autowired
	public ExportService(SimpMessagingTemplate template, ModelService modelService) {
		this.template = template;
		this.modelService = modelService;
	}

	@Async
	public void runExportsAsync(DatabaseClient client, String username, List<String> entityNames) throws IOException {
		runExports(client, username, entityNames);
	}

	public void runExports(DatabaseClient client, String username, List<String> entityNames) throws IOException {
		File zipFile = File.createTempFile("export", ".zip");
		ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

		for (int i = 0; i < entityNames.size(); i++) {
			String entityName = entityNames.get(i);
			try {
				ExportToCsvFileJob exportJob = new ExportToCsvFileJob(modelService.getModel(client), entityName);
				exportJob.run(client);

				File exportFile = exportJob.getExportFile();
				zipOutputStream.putNextEntry(new ZipEntry(entityName + ".csv"));
				zipOutputStream.write(FileUtils.readFileToByteArray(exportFile));
				zipOutputStream.closeEntry();
				exportFile.delete();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		zipOutputStream.close();

		saveExportInfo(client, new ExportPojo(zipFile.getAbsolutePath(), String.join(",", entityNames), username));
	}

	private String pojoUri(String username, String id) {
		return "/envision/exports/" + username + "/" + id + ".json";
	}

	public void saveExportInfo(DatabaseClient client, ExportPojo exportPojo) {
		JSONDocumentManager mgr = client.newJSONDocumentManager();
		DocumentMetadataHandle meta = new DocumentMetadataHandle();
		meta.getCollections().addAll(EXPORT_INFO_COLLECTION, USER_EXPORT_COLLECTION + exportPojo.username);
		JacksonHandle handle = new JacksonHandle(objectMapper.valueToTree(exportPojo));
		mgr.write(pojoUri(exportPojo.username, exportPojo.id), meta, handle);
		this.template.convertAndSend("/topic/status", StatusMessage.newStatus(exportPojo.name).withMessage("Export Complete").withPercentComplete(100));
	}

	public List<ExportInfo> getExports(HubClient hubClient) {
		QueryManager queryMgr = hubClient.getFinalClient().newQueryManager();
		JSONDocumentManager mgr = hubClient.getFinalClient().newJSONDocumentManager();

		StructuredQueryBuilder qb = queryMgr.newStructuredQueryBuilder();
		PojoQueryDefinition query = qb.collection(USER_EXPORT_COLLECTION + hubClient.getUsername());
		DocumentPage page =  mgr.search(query, 1);

		List<ExportInfo> exports = new ArrayList<>();
		page.forEach(documentRecord -> {
			try {
				JsonNode node = documentRecord.getContent(new JacksonHandle()).get();
				exports.add(new ExportInfo(objectMapper.treeToValue(node, ExportPojo.class)));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		});

		return exports;
	}

	public void removeExport(DatabaseClient client, String username, String exportId) throws JsonProcessingException {
		File zipFile = getFile(client, username, exportId);
		zipFile.delete();
		JSONDocumentManager mgr = client.newJSONDocumentManager();
		mgr.delete(pojoUri(username, exportId));
	}

	public File getFile(DatabaseClient client, String username, String exportId) throws JsonProcessingException {
		JSONDocumentManager mgr = client.newJSONDocumentManager();
		DocumentPage page = mgr.read(pojoUri(username, exportId));

		if (!page.hasNext()) {
			throw new NotFoundException();
		}
		DocumentRecord documentRecord = page.next();
		JsonNode node = documentRecord.getContent(new JacksonHandle()).get();
		ExportPojo exportPojo = objectMapper.treeToValue(node, ExportPojo.class);
		return new File(exportPojo.zipUri);
	}
}
