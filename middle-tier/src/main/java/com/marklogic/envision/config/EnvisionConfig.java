package com.marklogic.envision.config;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.ext.DatabaseClientConfig;
import com.marklogic.client.ext.SecurityContextType;
import com.marklogic.mgmt.ManageClient;
import com.marklogic.mgmt.ManageConfig;
import com.marklogic.mgmt.admin.AdminConfig;
import com.marklogic.mgmt.admin.AdminManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class EnvisionConfig {
	@Value("${marklogic.host:localhost}")
	public String markLogicHost;

	@Value("${marklogic.port:8002}")
	public Integer markLogicPort;

	@Value("${marklogic.username:admin}")
	public String marklogicUsername;

	@Value("${marklogic.password:admin}")
	public String marklogicPassword;

	@Value("${marklogic.database:}")
	public String marklogicDatabase;

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
		ManageConfig config = new ManageConfig(markLogicHost, markLogicPort, marklogicUsername, marklogicPassword);
		return new ManageClient(config);
	}

	public AdminManager getAdminManager() {
		AdminConfig config = new AdminConfig(markLogicHost, 8001, marklogicUsername, marklogicPassword);
		return new AdminManager(config);
	}

	public String getInstalledVersion(DatabaseClient client) {
		try {
			EvalResultIterator result = client.newServerEval().javascript("require('/envision/config.sjs').version").eval();
			if (result.hasNext()) {
				return result.next().getString();
			}
		}
		catch(FailedRequestException e) {}
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
