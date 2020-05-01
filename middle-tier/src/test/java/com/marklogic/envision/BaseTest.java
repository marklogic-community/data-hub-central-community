package com.marklogic.envision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.appdeployer.AppConfig;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.ext.DatabaseClientConfig;
import com.marklogic.client.ext.SecurityContextType;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.envision.services.DeployService;
import com.marklogic.grove.boot.error.NotAuthenticatedException;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.HubProject;
import com.marklogic.hub.impl.HubConfigImpl;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Component
public class BaseTest {
	@Autowired
	private HubConfigImpl hubConfig;

	@Autowired
	private HubProject hubProject;

	@Autowired
	private DeployService deployService;

	@Value("${dhfDir}")
	private File dhfDir;

	@Value("${dhfEnv}")
	private String dhfEnv;

	@Value("${mlUsername}")
	private String username;

	@Value("${mlPassword}")
	private String password;

	private boolean configFinished = false;

	HubConfigImpl getHubConfig() {
		if (!configFinished) {
			String projectDir = dhfDir.getAbsolutePath();
			hubProject.createProject(projectDir);
			hubConfig.setMlUsername(username);
			hubConfig.setMlPassword(password);
			hubConfig.resetAppConfigs();
			String envName = dhfEnv;
			if (envName == null || envName.isEmpty()) {
				envName = "local";
			}
			System.out.println("envName: " + envName);
			hubConfig.withPropertiesFromEnvironment(envName);
			hubConfig.resetHubConfigs();
			hubConfig.refreshProject();
			hubConfig.getAppConfig().setAppServicesUsername(username);
			hubConfig.getAppConfig().setAppServicesPassword(password);
			configFinished = true;
		}
		return hubConfig;
	}

	protected ObjectMapper om = new ObjectMapper();

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
		HubConfigImpl hubConfig = getHubConfig();
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

	protected static File getResourceFile(String resourceName) {
		return new File(BaseTest.class.getClassLoader().getResource(resourceName).getFile());
	}

	protected InputStream getResourceStream(String resourceName) {
		return BaseTest.class.getClassLoader().getResourceAsStream(resourceName);
	}

	protected String getResource(String resourceName) {
		InputStream inputStream = null;
		String output = null;
		try {
			inputStream = getResourceStream(resourceName);
			output = IOUtils.toString(inputStream);
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
		return output;
	}

	protected void installFinalDoc(String resource, String uri, String... collections) {
		FileHandle handle = new FileHandle(getResourceFile(resource));
		DocumentMetadataHandle meta = new DocumentMetadataHandle();
		meta.getCollections().addAll(collections);
		getFinalClient().newDocumentManager().write(uri, meta, handle);
	}

	protected void clearStagingFinalAndJobDatabases() {
		clearDatabases(HubConfig.DEFAULT_FINAL_NAME, HubConfig.DEFAULT_STAGING_NAME, HubConfig.DEFAULT_JOB_NAME);
	}

	public void clearDatabases(String... databases) {
		ServerEvaluationCall eval = getStagingClient().newServerEval();
		String installer =
			"declare variable $databases external;\n" +
				"for $database in fn:tokenize($databases, \",\")\n" +
				"return\n" +
				"  xdmp:eval('\n" +
				"    cts:uris() ! xdmp:document-delete(.)\n" +
				"  ',\n" +
				"  (),\n" +
				"  map:entry(\"database\", xdmp:database($database))\n" +
				"  )";
		eval.addVariable("databases", String.join(",", databases));
		EvalResultIterator result = eval.xquery(installer).eval();
		if (result.hasNext()) {
			logger.error(result.next().getString());
		}
	}

	public void installEnvisionModules() {
		deployService.deploy();
	}

	protected Logger logger = LoggerFactory.getLogger(getClass());
}
