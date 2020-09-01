package com.marklogic.envision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.command.Command;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.ext.DatabaseClientConfig;
import com.marklogic.client.ext.SecurityContextType;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.envision.deploy.DeployService;
import com.marklogic.grove.boot.error.NotAuthenticatedException;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.HubProject;
import com.marklogic.hub.deploy.commands.LoadHubModulesCommand;
import com.marklogic.hub.impl.DataHubImpl;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.mgmt.api.API;
import com.marklogic.mgmt.api.security.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class BaseTest {
	public static final String PROJECT_PATH = "ye-olde-project";

	public static Path projectPath;

	@Autowired
	private HubConfigImpl hubConfig;

	@Autowired
	private HubProject hubProject;

	@Autowired
	private DeployService deployService;

	@Autowired
	protected DataHubImpl dataHub;

	@Autowired
	protected LoadHubModulesCommand loadHubModulesCommand;

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
		String output;
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

	public Path createProjectDir() throws IOException {
		return createProjectDir(PROJECT_PATH);
	}

	// this method creates a project dir and copies the gradle.properties in.
	public Path createProjectDir(String projectDirName) throws IOException {
		projectPath = Files.createTempDirectory(projectDirName);
		File projectDir = projectPath.toFile();

		// force module loads for new test runs.
		File timestampDirectory = new File(projectDir + "/.tmp");
		if ( timestampDirectory.exists() ) {
			try {
				FileUtils.forceDelete(timestampDirectory);
			} catch (Exception ex) {
				logger.warn("Unable to delete .tmp directory: " + ex.getMessage());
			}
		}

		File finalTimestampDirectory = new File( "build/ml-javaclient-util");
		if ( finalTimestampDirectory.exists() ) {
			try {
				FileUtils.forceDelete(finalTimestampDirectory);
			} catch (Exception ex) {
				logger.warn("Unable to delete build/ml-javaclient-util directory: " + ex.getMessage());
			}
		}

		try {
			Path devProperties = getResourceFile("gradle.properties").toPath();
			Path projectProperties = projectDir.toPath().resolve("gradle.properties");
			Files.copy(devProperties, projectProperties, REPLACE_EXISTING);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return projectPath;
		// note at this point the properties from the project have not been  read.  maybe
		// props reading should be in this directory...
	}

	public void deleteProjectDir() {
		File projectPath = new File(PROJECT_PATH);
		if (projectPath.exists()) {
			try {
				FileUtils.forceDelete(projectPath);
			} catch (IOException e) {
				logger.warn("Unable to delete the project directory", e);
			}
		}
	}

	protected void installHubModules() {
		logger.debug("Installing Data Hub modules into MarkLogic");
		List<Command> commands = new ArrayList<>();
		commands.add(loadHubModulesCommand);

		SimpleAppDeployer deployer = new SimpleAppDeployer(getHubConfig().getManageClient(), getHubConfig().getAdminManager());
		deployer.setCommands(commands);
		deployer.deploy(getHubConfig().getAppConfig());
	}

	public void installEnvisionModules() {
		deployService.deploy();
	}

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected void init() throws IOException {
		Path projectPath = createProjectDir();
		hubConfig.createProject(projectPath.toAbsolutePath().toString());
		hubConfig.refreshProject();
		if(! hubProject.isInitialized()) {
			hubConfig.initHubProject();
		}

		try {
			File projectDir = projectPath.toFile();
			Path devProperties = getResourceFile("final-database.json").toPath();
			Path projectProperties = projectDir.toPath().resolve("src/main/ml-config/databases/final-database.json");
			Files.copy(devProperties, projectProperties, REPLACE_EXISTING);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		hubConfig.refreshProject();
	}

	public void teardownProject() {
		deleteProjectDir();
	}

	@PostConstruct
	public void bootstrapHub() throws IOException {
		teardownProject();
		init();


		boolean isInstalled = false;
		try {
			isInstalled = dataHub.isInstalled().isInstalled();
		} catch (Exception e) {
			logger.info("Datahub is not installed");
		}

		if (!isInstalled) {
			dataHub.install();

			User dataHubDeveloper = new User(new API(getHubConfig().getManageClient()), "test-data-hub-developer");
			dataHubDeveloper.setPassword("password");
			dataHubDeveloper.addRole("data-hub-developer");
			dataHubDeveloper.save();

			User dataHubOperator = new User(new API(getHubConfig().getManageClient()), "test-data-hub-operator");
			dataHubOperator.setPassword("password");
			dataHubOperator.addRole("data-hub-operator");
			dataHubOperator.save();

			User testAdmin = new User(new API(getHubConfig().getManageClient()), "test-admin-for-data-hub-tests");
			testAdmin.setDescription("This user is intended to be used by DHF tests that require admin or " +
				"admin-like capabilities, such as being able to deploy a DHF application");
			testAdmin.setPassword("password");
			testAdmin.addRole("admin");
			testAdmin.save();
		}

		if (getHubConfig().getIsProvisionedEnvironment()) {
			installHubModules();
		}
	}
}
