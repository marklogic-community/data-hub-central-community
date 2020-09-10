package com.marklogic.envision.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.client.ext.modulesloader.Modules;
import com.marklogic.client.ext.modulesloader.ModulesFinder;
import com.marklogic.client.ext.modulesloader.impl.EntityDefModulesFinder;
import com.marklogic.client.ext.util.DefaultDocumentPermissionsParser;
import com.marklogic.client.ext.util.DocumentPermissionsParser;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.mgmt.util.ObjectMapperFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

public class DeployEntitiesCommand extends LoggingObject {

	private final HubClient hubClient;

	private final DocumentPermissionsParser documentPermissionsParser = new DefaultDocumentPermissionsParser();
	private final ObjectMapper objectMapper;


	public DeployEntitiesCommand(HubClient hubClient) {
		this.hubClient = hubClient;
		this.objectMapper = ObjectMapperFactory.getObjectMapper();
	}

	public void execute() {
		DatabaseClient stagingClient = hubClient.getStagingClient();
		DatabaseClient finalClient = hubClient.getFinalClient();

		Path entitiesPath = hubClient.getHubConfig().getHubEntitiesDir();

		JSONDocumentManager finalDocMgr = finalClient.newJSONDocumentManager();
		JSONDocumentManager stagingDocMgr = stagingClient.newJSONDocumentManager();

		DocumentWriteSet finalEntityDocumentWriteSet = finalDocMgr.newWriteSet();
		DocumentWriteSet stagingEntityDocumentWriteSet = stagingDocMgr.newWriteSet();
		DeployEntitiesCommand.ResourceToURI entityResourceToURI = new DeployEntitiesCommand.ResourceToURI(){
			public String toURI(Resource r) {
				return "/entities/" + r.getFilename();
			}
		};
		EntityDefModulesFinder entityDefModulesFinder = new EntityDefModulesFinder();
		try {
			//first let's do the entities paths
			if (entitiesPath.toFile().exists()) {
				Files.walkFileTree(entitiesPath, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						executeWalk(
							dir,
							entityDefModulesFinder,
							entityResourceToURI,
							buildMetadataForEntityModels(),
							stagingEntityDocumentWriteSet,
							finalEntityDocumentWriteSet
						);
						return FileVisitResult.CONTINUE;
					}
				});
			}

			if (stagingEntityDocumentWriteSet.size() > 0) {
				finalDocMgr.write(finalEntityDocumentWriteSet);
				stagingDocMgr.write(stagingEntityDocumentWriteSet);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * As of 5.1.0, entity model permissions are separate from module permissions. Though if entity model permissions
	 * are not defined, then this falls back to using module permissions.
	 */
	protected DocumentMetadataHandle buildMetadataForEntityModels() {
		String permissions = hubClient.getHubConfig().getEntityModelPermissions();
		if (permissions == null || permissions.trim().length() < 1) {
			if (logger.isInfoEnabled()) {
				logger.info("Entity model permissions were not set, so using module permissions; consider setting mlEntityModelPermissions " +
					"in case you want entity models to have custom permissions.");
			}
			permissions = hubClient.getHubConfig().getModulePermissions();
		}
		return buildMetadata("http://marklogic.com/entity-services/models", permissions);
	}

	private void executeWalk(
		Path dir,
		ModulesFinder modulesFinder,
		DeployEntitiesCommand.ResourceToURI resourceToURI,
		DocumentMetadataHandle metadata,
		DocumentWriteSet... writeSets
	) throws IOException {
		Modules modules = modulesFinder.findModules(dir.toString());
		for (Resource r : modules.getAssets()) {
			addResourceToWriteSets(
				r,
				resourceToURI.toURI(r),
				metadata,
				writeSets
			);
		}
	}
	//5.2 lib makes this method protected in base class so mut change to public
	//private DocumentMetadataHandle buildMetadata(String collection, String permissions) {
	public DocumentMetadataHandle buildMetadata(String collection, String permissions) {
			DocumentMetadataHandle meta = new DocumentMetadataHandle();
		meta.getCollections().add(collection);
		documentPermissionsParser.parsePermissions(permissions, meta.getPermissions());
		return meta;
	}

	private void addResourceToWriteSets(
		Resource r,
		String docId,
		DocumentMetadataHandle meta,
		DocumentWriteSet... writeSets
	) throws IOException {
		InputStream inputStream = r.getInputStream();

		JsonNode json;
		try {
			json = objectMapper.readTree(inputStream);
		} finally {
			inputStream.close();
		}

		if (json instanceof ObjectNode && json.has("language")) {
			json = replaceLanguageWithLang((ObjectNode)json);
			try {
				objectMapper.writeValue(r.getFile(), json);
			} catch (Exception ex) {
				logger.warn("Unable to replace 'language' with 'lang' in artifact file: " + r.getFile().getAbsolutePath()
					+ ". You should replace 'language' with 'lang' yourself in this file. Error cause: " + ex.getMessage(), ex);
			}
		}

		for (DocumentWriteSet writeSet : writeSets) {
			writeSet.add(docId, meta, new JacksonHandle(json));
		}
	}

	/**
	 * Per DHFPROD-3193 and an update to MarkLogic 10.0-2, "lang" must now be used instead of "language". To ensure that
	 * a user artifact is never loaded with "language", this command handles both updating the JSON that will be loaded
	 * into MarkLogic and updating the artifact file.
	 */
	protected ObjectNode replaceLanguageWithLang(ObjectNode object) {
		ObjectNode newObject = objectMapper.createObjectNode();
		newObject.put("lang", object.get("language").asText());
		Iterator<String> fieldNames = object.fieldNames();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			if (!"language".equals(fieldName)) {
				newObject.set(fieldName, object.get(fieldName));
			}
		}
		return newObject;
	}

	abstract static class ResourceToURI {
		public abstract String toURI(Resource r) throws IOException;
	}
}
