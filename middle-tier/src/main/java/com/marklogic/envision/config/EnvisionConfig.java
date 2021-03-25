package com.marklogic.envision.config;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.ext.DatabaseClientConfig;
import com.marklogic.client.ext.SecurityContextType;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.hub.impl.HubClientImpl;
import com.marklogic.envision.hub.impl.MultiTenantProjectImpl;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.HubProject;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.hub.impl.HubProjectImpl;
import com.marklogic.mgmt.ManageClient;
import com.marklogic.mgmt.ManageConfig;
import com.marklogic.mgmt.admin.AdminConfig;
import com.marklogic.mgmt.admin.AdminManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@PropertySource({"classpath:application.properties"})
public class EnvisionConfig {
	@Value("${envision.autoInstall:true}")
	public boolean autoInstall;

	@Value("${marklogic.host:localhost}")
	public String markLogicHost;

	@Value("${marklogic.port:8002}")
	public Integer markLogicPort;

	@Value("${marklogic.managePort:8002}")
	public Integer markLogicManagePort;

	@Value("${marklogic.adminPort:8001}")
	public Integer markLogicAdminPort;

	@Value("${marklogic.username:admin}")
	public String marklogicUsername;

	@Value("${marklogic.password:admin}")
	public String marklogicPassword;

	@Value("${marklogic.database:}")
	public String marklogicDatabase;

	@Value("${dhfDir:.}")
	public File dhfDir;

	@Value("${dhfEnv:local}")
	public String dhfEnv;

	private boolean isMultiTenant;

	public boolean isMultiTenant() {
		return isMultiTenant;
	}

	@Value("${envision.multiTenant:false}")
	public void setMultiTenant(boolean multiTenant) {
		isMultiTenant = multiTenant;
	}

	private final Environment environment;

	private final HubConfigImpl hubConfig;
	private final HubProject hubProject;

	public HubConfigImpl getHubConfig() {
		return hubConfig;
	}

	@Autowired
	EnvisionConfig(Environment environment) {
		this.environment = environment;
		this.hubProject = new HubProjectImpl();
		this.hubConfig = new HubConfigImpl(hubProject);
	}


	public HubClient newAdminHubClient() {
		return newHubClient(marklogicUsername, marklogicPassword);
	}

	public HubClient newHubClient(String username, String password) {
		HubConfigImpl hubConfig = newHubConfig(username, password);

		Map<DatabaseKind, String> databaseNames = new HashMap<>();
		DatabaseKind[] kinds = new DatabaseKind[]{ DatabaseKind.STAGING, DatabaseKind.FINAL, DatabaseKind.JOB, DatabaseKind.MODULES, DatabaseKind.STAGING_TRIGGERS, DatabaseKind.STAGING_SCHEMAS, DatabaseKind.FINAL_TRIGGERS, DatabaseKind.FINAL_SCHEMAS };
		for(DatabaseKind kind: kinds) {
			databaseNames.put(kind, hubConfig.getDbName(kind));
		}

		return new HubClientImpl(hubConfig, databaseNames, this.isMultiTenant());
	}

	private HubConfigImpl newHubConfig(String username, String password) {
		HubProject hp = hubProject;
		if (isMultiTenant) {
			hp = new MultiTenantProjectImpl(hubProject, username);
		}
		HubConfigImpl hubConfig = new HubConfigImpl(hp);
		setupHubConfig(hubConfig, username, password);
		return hubConfig;
	}

	public HubConfigImpl getAdminHubConfig() {
		return this.hubConfig;
	}

	public DatabaseClient newAdminFinalClient() {
		HubConfigImpl hubConfig = newHubConfig(marklogicUsername, marklogicPassword);
		AppConfig appConfig = hubConfig.getAppConfig();
		DatabaseClientConfig config = new DatabaseClientConfig(appConfig.getHost(), hubConfig.getPort(DatabaseKind.FINAL), hubConfig.getMlUsername(), hubConfig.getMlPassword());
		config.setSecurityContextType(SecurityContextType.valueOf(hubConfig.getAuthMethod(DatabaseKind.FINAL).toUpperCase()));
		config.setSslHostnameVerifier(hubConfig.getSslHostnameVerifier(DatabaseKind.FINAL));
		config.setSslContext(hubConfig.getSslContext(DatabaseKind.FINAL));
		config.setCertFile(hubConfig.getCertFile(DatabaseKind.FINAL));
		config.setCertPassword(hubConfig.getCertPassword(DatabaseKind.FINAL));
		config.setExternalName(hubConfig.getExternalName(DatabaseKind.FINAL));
		config.setTrustManager(hubConfig.getTrustManager(DatabaseKind.FINAL));
		if (hubConfig.getIsHostLoadBalancer()) {
			config.setConnectionType(DatabaseClient.ConnectionType.GATEWAY);
		}
		return appConfig.getConfiguredDatabaseClientFactory().newDatabaseClient(config);
	}

	@PostConstruct
	public void configureHub() {
		setupHubConfig(hubConfig, marklogicUsername, marklogicPassword);
	}

	private void setupHubConfig(HubConfigImpl hubConfig, String username, String password) {
		try {
			String projectDir = dhfDir.getCanonicalPath();
			hubProject.createProject(projectDir);
			String envName = dhfEnv;
			if (envName == null || envName.isEmpty()) {
				envName = "local";
			}
			hubConfig.withPropertiesFromEnvironment(envName);
			hubConfig.applyProperties(buildPropertySource(username, password));
			hubConfig.setMlUsername(username);
			hubConfig.setMlPassword(password);
			if (isMultiTenant()) {
				String roleName = DigestUtils.md5Hex(username);
				hubConfig.setEntityModelPermissions(String.format("%s,read,%s,update", roleName, roleName));
			}
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Construct a PropertySource based on the properties in the Spring Boot environment plus the given username and
	 * password, which are supplied when a user authenticates.
	 *
	 * @param username
	 * @param password
	 * @return
	 */
	protected com.marklogic.mgmt.util.PropertySource buildPropertySource(String username, String password) {
		Properties primaryProperties = new Properties();
		primaryProperties.setProperty("mlUsername", username);
		primaryProperties.setProperty("mlPassword", password);

		return propertyName -> {
			String value = primaryProperties.getProperty(propertyName);
			if (value != null) {
				return value;
			}
			return environment.getProperty(propertyName);
		};
	}

	public DatabaseClient getClient() {
		DatabaseClientConfig config = new DatabaseClientConfig(markLogicHost, markLogicPort, marklogicUsername, marklogicPassword);
		config.setSecurityContextType(SecurityContextType.DIGEST);
		if (marklogicDatabase.length() > 0) {
			config.setDatabase(marklogicDatabase);
		}
		AppConfig appConfig = new AppConfig();
		appConfig.setHost(markLogicHost);
		return appConfig.getConfiguredDatabaseClientFactory().newDatabaseClient(config);
	}

	public ManageClient getManageClient() {
		ManageConfig config = new ManageConfig(markLogicHost, markLogicManagePort, marklogicUsername, marklogicPassword);
		return new ManageClient(config);
	}

	public AdminManager getAdminManager() {
		AdminConfig config = new AdminConfig(markLogicHost, markLogicAdminPort, marklogicUsername, marklogicPassword);
		return new AdminManager(config);
	}

	public DatabaseClient getModulesClient() {
		return hubConfig.newModulesDbClient();
	}

	public String getInstalledVersion() {
		try {
			EvalResultIterator result = hubConfig.newModulesDbClient().newServerEval().javascript("require('/envision/config.sjs').version").eval();
			if (result.hasNext()) {
				return result.next().getString();
			}
		}
		catch(Exception e) {}
		return null;
	}

	public String getVersion() {
		Properties properties = new Properties();
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("version.properties")) {
			properties.load(inputStream);
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		String version = (String)properties.get("version");

		// this lets debug builds work from an IDE
		if (version.equals("${project.version}")) {
			version = "1.0.4";
		}
		return version;
	}
}
