package com.marklogic.envision.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ResourceNotFoundException;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.pojo.PojoQueryDefinition;
import com.marklogic.client.query.MatchDocumentSummary;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.envision.config.EnvisionConfig;
import com.marklogic.envision.dataServices.Users;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.model.ModelService;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

	private final EnvisionConfig envisionConfig;
	private final ApplicationEventPublisher eventPublisher;
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ModelService modelService;

	@Autowired
	UserService(ModelService modelService, EnvisionConfig envisionConfig, ApplicationEventPublisher eventPublisher) {
		this.modelService = modelService;
		this.envisionConfig = envisionConfig;
		this.eventPublisher = eventPublisher;
	}

	private String userUri(String email) {
		String userId = DigestUtils.md5Hex(email);
		return "/envision/users/" + userId + ".json";
	}

	public boolean userExists(String email) {
		JSONDocumentManager mgr = envisionConfig.getAdminHubConfig().newFinalClient().newJSONDocumentManager();
		return mgr.exists(userUri(email)) != null;
	}

	public UserPojo getUser(DatabaseClient client, String email) {
		try {
			JSONDocumentManager mgr = client.newJSONDocumentManager();
			JacksonHandle handle = mgr.read(userUri(email), new JacksonHandle());
			if (handle != null) {
				return objectMapper.treeToValue(handle.get(), UserPojo.class);
			}
		}
		catch(JsonProcessingException|ResourceNotFoundException e) {}
		return null;
	}

	public UserPojo createUser(UserPojo user) throws IOException {
		Users.on(envisionConfig.newAdminFinalClient()).createUser(objectMapper.valueToTree(user));
		user.token = UUID.randomUUID().toString();

		final Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, 2);
		user.tokenExpiry = c.getTime();
		user.validated = false;

		saveUser(user);

		modelService.saveModelFile(envisionConfig.isMultiTenant(), user.email, objectMapper.readTree("{\"name\":\"My Model\",\"edges\":{},\"nodes\":{}}"));

		eventPublisher.publishEvent(new OnRegistrationCompleteEvent(user));
		return user;
	}

	public void saveUser(UserPojo user) {
		JSONDocumentManager mgr = envisionConfig.getAdminHubConfig().newFinalClient().newJSONDocumentManager();
		DocumentMetadataHandle meta = new DocumentMetadataHandle();
		meta.getCollections().addAll("http://marklogic.com/envision/usr");
		meta.getPermissions().add("envision", DocumentMetadataHandle.Capability.READ, DocumentMetadataHandle.Capability.UPDATE);
		JacksonHandle handle = new JacksonHandle(objectMapper.valueToTree(user));
		mgr.write(userUri(user.email), meta, handle);
	}

	public JsonNode getUsers() {
		return Users.on(envisionConfig.newAdminFinalClient()).getUsers();
	}

	public void deleteUser(String username) throws IOException {
		HubClient hubClient = envisionConfig.newAdminHubClient();
		modelService.deleteAllModels(hubClient, username);
		Users.on(hubClient.getFinalClient()).deleteUser(username);
	}

	public void addResetTokenToUser(String email) {
		try {
			UserPojo user = getUser(envisionConfig.getAdminHubConfig().newFinalClient(), email);
			user.resetToken = UUID.randomUUID().toString();
			final Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.DATE, 2);
			user.resetTokenExpiry = c.getTime();

			saveUser(user);
			logger.info("token: " + user.resetToken);

			eventPublisher.publishEvent(new OnResetPasswordEvent(user));
		}
		catch(ResourceNotFoundException e) {}
	}

	private UserPojo getUserFromResetToken(String token) {
		DatabaseClient client = envisionConfig.getAdminHubConfig().newFinalClient();
		QueryManager queryMgr = client.newQueryManager();
		JSONDocumentManager mgr = client.newJSONDocumentManager();
		StructuredQueryBuilder qb = queryMgr.newStructuredQueryBuilder();
		PojoQueryDefinition query = qb.value(qb.jsonProperty("resetToken"), token);
		MatchDocumentSummary mds = queryMgr.findOne(query);
		if (mds != null) {
			try {
				return objectMapper.treeToValue(mgr.read(mds.getUri(), new JacksonHandle()).get(), UserPojo.class);
			}
			catch(JsonProcessingException e) {}
		}
		return null;
	}

	public ValidateTokenPojo validateResetToken(String token) {
		ValidateTokenPojo vtp = new ValidateTokenPojo();
		vtp.valid = false;
		vtp.error = "Invalid Token";
		UserPojo user = getUserFromResetToken(token);
		if (user != null && user.resetToken.equals(token)) {
			if (user.resetTokenExpiry.before(new Date())) {
				vtp.error = "Token Expired";
			}
			else {
				vtp.valid = true;
				vtp.error = null;
				vtp.email = user.email;
			}
		}

		return vtp;
	}

	public ValidateTokenPojo validateToken(String token) {
		DatabaseClient client = envisionConfig.getAdminHubConfig().newFinalClient();
		QueryManager queryMgr = client.newQueryManager();
		JSONDocumentManager mgr = client.newJSONDocumentManager();

		StructuredQueryBuilder qb = queryMgr.newStructuredQueryBuilder();
		PojoQueryDefinition query = qb.and(
			qb.value(qb.jsonProperty("token"), token),
			qb.value(qb.jsonProperty("validated"), false)
		);
		MatchDocumentSummary mds = queryMgr.findOne(query);
		ValidateTokenPojo vtp = new ValidateTokenPojo();
		vtp.valid = false;
		vtp.error = "invalid token";
		if (mds != null) {
			try {
				UserPojo user = objectMapper.treeToValue(mgr.read(mds.getUri(), new JacksonHandle()).get(), UserPojo.class);
				boolean expired = user.tokenExpiry.before(new Date());
				boolean validToken = user.token.equals(token);

				if (validToken && !expired) {
					user.validated = true;
					user.tokenExpiry = null;
					user.token = null;
					saveUser(user);
					vtp.email = user.email;
					vtp.valid = true;
					vtp.error = null;
				}
				else if (expired) {
					vtp.valid = false;
					vtp.error = "Expired Token";
				}
			}
			catch(JsonProcessingException e) {}
		}

		return vtp;
	}

	public void updatePassword(String token, String password) {
		UserPojo user = getUserFromResetToken(token);
		if (user != null) {
			Users.on(envisionConfig.newAdminFinalClient()).updatePassword(user.email, password);
			user.resetToken = null;
			user.resetTokenExpiry = null;
			saveUser(user);
		}
	}
}
