package com.marklogic.grove.boot;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ext.DatabaseClientConfig;
import com.marklogic.client.ext.SecurityContextType;
import com.marklogic.grove.boot.error.NotAuthenticatedException;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.impl.HubConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractController {

	final private HubConfigImpl hubConfig;

	@Autowired
	public AbstractController(HubConfigImpl hubConfig) {
		this.hubConfig = hubConfig;
	}

	public HubConfigImpl getHubConfig() {
		if (hubConfig != null) {
			return hubConfig;
		}

		throw new NotAuthenticatedException();
	}

	protected DatabaseClient getFinalClient() {
		return getClient(DatabaseKind.FINAL);
	}

	protected DatabaseClient getStagingClient() {
		return getClient(DatabaseKind.STAGING);
	}

	protected DatabaseClient getJobClient() {
		return getClient(DatabaseKind.JOB);
	}

	protected DatabaseClient getClient(DatabaseKind kind) {
		if (hubConfig != null) {
			AppConfig appConfig = hubConfig.getAppConfig();
			if (appConfig != null) {
				DatabaseClientConfig config = new DatabaseClientConfig(appConfig.getHost(), hubConfig.getPort(kind), hubConfig.getMlUsername(), hubConfig.getMlPassword());
				config.setSecurityContextType(SecurityContextType.valueOf(hubConfig.getAuthMethod(kind).toUpperCase()));
				config.setSslHostnameVerifier(hubConfig.getSslHostnameVerifier(kind));
				config.setSslContext(hubConfig.getSslContext(kind));
				config.setCertFile(hubConfig.getCertFile(kind));
				config.setCertPassword(hubConfig.getCertPassword(kind));
				config.setExternalName(hubConfig.getExternalName(kind));
				config.setTrustManager(hubConfig.getTrustManager(kind));
				if (hubConfig.getIsHostLoadBalancer()) {
					config.setConnectionType(DatabaseClient.ConnectionType.GATEWAY);
				}
				return appConfig.getConfiguredDatabaseClientFactory().newDatabaseClient(config);
			}
		}

		throw new NotAuthenticatedException();
	}
	protected Logger logger = LoggerFactory.getLogger(getClass());
}
