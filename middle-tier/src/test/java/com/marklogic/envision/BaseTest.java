package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.command.Command;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.ext.DatabaseClientConfig;
import com.marklogic.client.ext.SecurityContextType;
import com.marklogic.client.impl.FailedRequest;
import com.marklogic.client.io.*;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.envision.auth.UserPojo;
import com.marklogic.envision.auth.UserService;
import com.marklogic.envision.config.EnvisionConfig;
import com.marklogic.envision.dataServices.Users;
import com.marklogic.envision.email.EmailService;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.installer.InstallService;
import com.marklogic.envision.model.ModelService;
import com.marklogic.grove.boot.Application;
import com.marklogic.grove.boot.error.NotAuthenticatedException;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.HubProject;
import com.marklogic.hub.deploy.commands.LoadHubArtifactsCommand;
import com.marklogic.hub.deploy.commands.LoadHubModulesCommand;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.impl.DataHubImpl;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.hub.step.impl.Step;
import com.marklogic.mgmt.api.API;
import com.marklogic.mgmt.api.security.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.comparator.JSONComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Application.class)
@ActiveProfiles("test")
public class BaseTest {
	public static final String ADMIN_ACCOUNT_NAME = "admin.smith@marklogic.com";
	public static final String ACCOUNT_NAME = "bob.smith@marklogic.com";
	public static final String ACCOUNT_PASSWORD = "password";
	public static final String ACCOUNT_NAME2 = "jim.jones@marklogic.com";
	public static final String PROJECT_PATH = "ye-olde-project";

	public static Path projectPath;

	@Autowired
	protected EnvisionConfig envisionConfig;

	@Autowired
	private HubConfig hubConfig;

	private HubProject hubProject;

	@Autowired
	private InstallService installService;

	@MockBean
	protected EmailService emailService;

	@Autowired
	protected UserService userService;

	@Autowired
	protected DataHubImpl dataHub;

	@Autowired
	protected LoadHubModulesCommand loadHubModulesCommand;

	@Autowired
	protected LoadHubArtifactsCommand loadHubArtifactsCommand;

	@Autowired
	protected ModelService modelService;

	private File dhfDir;

	@Value("${dhfEnv}")
	private String dhfEnv;

	@Value("${mlUsername}")
	private String username;

	@Value("${mlPassword}")
	private String password;

	private boolean configFinished = false;

	@BeforeEach
	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);
		installEnvisionModules();
	}

	protected HubConfigImpl getHubConfig() {
		HubConfigImpl hubConfigImpl = (HubConfigImpl) hubConfig;
		if (!configFinished) {
			String projectDir = dhfDir.getAbsolutePath();
			hubProject.createProject(projectDir);
			if (!hubProject.isInitialized()) {
				hubConfig.initHubProject();
			}
			hubConfigImpl.setMlUsername(username);
			hubConfigImpl.setMlPassword(password);
			String envName = dhfEnv;
			if (envName == null || envName.isEmpty()) {
				envName = "local";
			}
			System.out.println("envName: " + envName);
			hubConfigImpl.withPropertiesFromEnvironment(envName);
			hubConfigImpl.refreshProject();
			hubConfigImpl.getAppConfig().setAppServicesUsername(username);
			hubConfigImpl.getAppConfig().setAppServicesPassword(password);
			configFinished = true;
		}
		return (HubConfigImpl) hubConfigImpl;
	}

	protected void registerAccount() throws IOException {
		registerAccount(ACCOUNT_NAME, ACCOUNT_PASSWORD);
	}

	protected void registerAccount(String email, String password) throws IOException {
		UserPojo user = new UserPojo();
		user.email = email;
		user.password = password;
		user.name = "Bob Smith";
		user = userService.createUser(user);
		userService.validateToken(user.token);
		Mockito.reset(emailService);
	}

	protected final ObjectMapper objectMapper = new ObjectMapper();

	protected HubClient getAdminHubClient() {
		return getHubClient(username, password);
	}
	protected HubClient getNonAdminHubClient() { return getHubClient(ACCOUNT_NAME, ACCOUNT_PASSWORD); }

	protected HubClient getHubClient(String username, String password) {
		return envisionConfig.newHubClient(username, password);
	}

	protected DatabaseClient getFinalClient() {
		return getClient(DatabaseKind.FINAL);
	}

	protected DatabaseClient getStagingClient() {
		return getClient(DatabaseKind.STAGING);
	}

	protected DatabaseClient getStagingModulesClient() {
		HubConfigImpl hubConfig = getHubConfig();
		if (hubConfig != null) {
			AppConfig appConfig = hubConfig.getAppConfig();
			if (appConfig != null) {
				return DatabaseClientFactory.newClient(appConfig.getHost(), hubConfig.getPort(DatabaseKind.STAGING), HubConfig.DEFAULT_MODULES_DB_NAME, new DatabaseClientFactory.DigestAuthContext(hubConfig.getMlUsername(), hubConfig.getMlPassword()), DatabaseClient.ConnectionType.GATEWAY);
			}
		}
		return null;
	}

	protected DatabaseClient getJobClient() {
		return getClient(DatabaseKind.JOB);
	}

	protected DatabaseClient getClient(DatabaseKind kind) {
		HubConfigImpl hubConfig = getNonAdminHubClient().getHubConfig();
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

	protected byte[] getResourceBytes(String resourceName) {
		try {
			return IOUtils.toByteArray(BaseTest.class.getClassLoader().getResourceAsStream(resourceName));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected byte[] getDocumentBytes(DatabaseClient client, String uri) {
		DocumentPage page = client.newDocumentManager().read(uri);
		return page.next().getContent(new BytesHandle()).get();
	}

	protected String getDocumentString(DatabaseClient client, String uri) {
		DocumentPage page = client.newDocumentManager().read(uri);
		return page.next().getContent(new StringHandle()).get();
	}

	protected String getModulesFile(String uri) {
		try {
			String contents =  getStagingModulesClient().newDocumentManager().read(uri).next().getContent(new StringHandle()).get();
			return contents.replaceFirst("(\\(:|//)\\s+cache\\sbuster:.+\\n", "");
		}
		catch (IllegalStateException e){
			return null;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
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

	protected String getCollectionDoc(DatabaseClient client, String collection, String query) {
		String rest = ")[1]";
		if (query != null) {
			rest = ", '" + query + "')[1]";
		}
		String xquery = "cts:search(fn:collection('" + collection + "')" + rest;
		EvalResultIterator it = client.newServerEval().xquery(xquery).eval();
		return it.next().getString();
	}

	protected void deleteProtectedPaths(DatabaseClient client) {
		client.newServerEval().xquery("import module namespace sec = \"http://marklogic.com/xdmp/security\" at \"/MarkLogic/security.xqy\";\n" +
			"      \n" +
			"xdmp:invoke-function(function() {\n" +
			"for $path in /sec:protected-path\n" +
			"return\n" +
			"  sec:unprotect-path($path/sec:path-expression, $path/sec:path-namespaces/sec:path-namespace)\n" +
			"}, map:entry(\"database\", xdmp:security-database())),\n" +
			"xdmp:invoke-function(function() {\n" +
			"for $path in /sec:protected-path\n" +
			"return\n" +
			"  sec:remove-path($path/sec:path-expression, $path/sec:path-namespaces/sec:path-namespace)\n" +
			"}, map:entry(\"database\", xdmp:security-database())),\n" +
			"xdmp:invoke-function(function() {\n" +
			"for $path in /sec:protected-path\n" +
			"return\n" +
			"  $path\n" +
			"}, map:entry(\"database\", xdmp:security-database()))").eval();

	}

	protected void deleteCollection(DatabaseClient client, String collection) {
		client.newServerEval().xquery("xdmp:collection-delete(\"" + collection + "\")").eval();

	}

	protected JsonNode getProtectedPaths(DatabaseClient client) throws IOException {
		EvalResultIterator it = client.newServerEval().xquery("xdmp:invoke-function(function() {\n" +
			"  json:to-array(/*:protected-path/*:path-expression/fn:string())\n" +
			"}, map:entry(\"database\", xdmp:security-database()))").eval();
		return objectMapper.readTree(it.next().getString());
	}

	protected void installFinalDoc(String resource, String uri, String... collections) {
		installDoc(getFinalClient(), resource, uri, collections);
	}

	protected void installDoc(DatabaseClient client, String resource, String uri, String... collections) {
		FileHandle handle = new FileHandle(getResourceFile(resource));
		DocumentMetadataHandle meta = new DocumentMetadataHandle();
		meta.getCollections().addAll(collections);
		client.newDocumentManager().write(uri, meta, handle);
	}

	protected void clearStagingFinalAndJobDatabases() {
		clearDatabases(HubConfig.DEFAULT_FINAL_NAME, HubConfig.DEFAULT_STAGING_NAME, HubConfig.DEFAULT_JOB_NAME);
	}

	public void clearDatabases(String... databases) {
		ServerEvaluationCall eval = getAdminHubClient().getStagingClient().newServerEval();
		String installer =
			"declare option xdmp:mapping \"false\";\n" +
			"declare variable $databases external;\n" +
			"for $database in fn:tokenize($databases, \",\")\n" +
			"return\n" +
			"  try {\n" +
			"    xdmp:eval('\n" +
			"      cts:uris() ! xdmp:document-delete(.)\n" +
			"    ',\n" +
			"    (),\n" +
			"    map:entry(\"database\", xdmp:database($database))\n" +
			"    )\n" +
			"  } catch($ex) {()}";;
		eval.addVariable("databases", String.join(",", databases));
		EvalResultIterator result = eval.xquery(installer).eval();
		if (result.hasNext()) {
			logger.error(result.next().getString());
		}
	}

	public void removeDoc(DatabaseClient client, String... uris) {
		client.newDocumentManager().delete(uris);
	}

	public void removeUser(String username) {
		Users.on(getAdminHubClient().getFinalClient()).deleteUser(username);
	}

	public Path createProjectDir() throws IOException {
		return createProjectDir(PROJECT_PATH);
	}

	// this method creates a project dir and copies the gradle.properties in.
	public Path createProjectDir(String projectDirName) throws IOException {
		projectPath = Files.createTempDirectory(projectDirName);
		File projectDir = projectPath.toFile();
		this.dhfDir = projectDir;
		envisionConfig.dhfDir = projectDir;
		modelService.clearCachedModelsDirFile();
		envisionConfig.configureHub();
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

			File testConfig = getResourceFile("test-config");
			File projectMlConfig = projectDir.toPath().resolve("src").resolve("main").resolve("ml-config").toFile();
			projectMlConfig.mkdirs();
			FileUtils.copyDirectory(testConfig, projectMlConfig);
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
		commands.add(loadHubArtifactsCommand);

		SimpleAppDeployer deployer = new SimpleAppDeployer(getHubConfig().getManageClient(), getHubConfig().getAdminManager());
		deployer.setCommands(commands);
		deployer.deploy(getHubConfig().getAppConfig());
	}

	public void installEnvisionModules() {

		if (!InstallTracker.hasInstalledModules()) {
			installHubModules();
		}
		String multiTenantValue = String.valueOf(envisionConfig.isMultiTenant());
		if (!(InstallTracker.hasInstalledModules() && InstallTracker.getLastMultiTenantValue().equals(multiTenantValue))) {
			installService.install(true);
			InstallTracker.setInstalledModules(true);
			InstallTracker.setLastMultiTenantValue(multiTenantValue);
			if (envisionConfig.isMultiTenant()) {
				try {
					registerAccount();
					registerAccount(ACCOUNT_NAME2, ACCOUNT_PASSWORD);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
	}

	public void clearModules() {
		clearDatabases(HubConfig.DEFAULT_MODULES_DB_NAME);
		InstallTracker.setInstalledModules(false);
	}

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected void init() throws IOException {
		Path projectPath = createProjectDir().toAbsolutePath();
		dhfDir = projectPath.toFile();
		envisionConfig.dhfDir = dhfDir;
		modelService.clearCachedModelsDirFile();
		hubConfig.createProject(projectPath.toString());
		hubConfig.refreshProject();
		hubProject = hubConfig.getHubProject();
		if(!hubProject.isInitialized()) {
			hubConfig.initHubProject();
		}

		try {
			Path devProperties = getResourceFile("final-database.json").toPath();
			Path projectProperties = projectPath.resolve("src/main/ml-config/databases/final-database.json");
			Files.copy(devProperties, projectProperties, REPLACE_EXISTING);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		getHubConfig();
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

	protected ArrayNode getEntities() {
		QueryManager mgr = getAdminHubClient().getFinalClient().newQueryManager();
		StructuredQueryBuilder sqb = mgr.newStructuredQueryBuilder();
		JsonNode response = mgr.search(sqb.collection("http://marklogic.com/entity-services/models"), new JacksonHandle()).get();
		return (ArrayNode)response.get("results");
	}

	protected int getEntityCount() {
		return getEntities().size();
	}

	protected int getDocCount(DatabaseClient client, String collection) {
		int count = 0;
		String collectionName = "";
		if (collection != null) {
			collectionName = "'" + collection + "'";
		}
		EvalResultIterator resultItr = client.newServerEval().xquery("xdmp:estimate(fn:collection(" + collectionName + "))").eval();
		if (resultItr == null || !resultItr.hasNext()) {
			return count;
		}
		EvalResult res = resultItr.next();
		count = Math.toIntExact((long) res.getNumber());
		return count;
	}

	protected int getDocCountFromUriPattern(DatabaseClient client, String uriPattern) {
		int count = 0;
		String pattern = "";
		if (uriPattern != null) {
			pattern = "'" + uriPattern + "'";
		}
		EvalResultIterator resultItr = client.newServerEval().xquery("fn:count(cts:uri-match(" + pattern + "))").eval();
		if (resultItr == null || !resultItr.hasNext()) {
			return count;
		}
		EvalResult res = resultItr.next();
		count = Math.toIntExact((long) res.getNumber());
		return count;
	}

	protected int getTriplesCount(DatabaseClient client) {
		int count = 0;
		EvalResultIterator resultItr = client.newServerEval().xquery("fn:count(cts:triples())").eval();
		if (resultItr == null || !resultItr.hasNext()) {
			return count;
		}
		EvalResult res = resultItr.next();
		count = Math.toIntExact((long) res.getNumber());
		return count;
	}

	protected void jsonAssertEquals(JsonNode expected, JsonNode actual) throws Exception {
		jsonAssertEquals(expected, actual,true);
	}

	protected void jsonAssertEquals(JsonNode expected, JsonNode actual, Boolean strict) throws Exception {
		jsonAssertEquals(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(actual), strict);
	}

	protected void jsonAssertEquals(JsonNode expected, String actual) throws Exception {
		jsonAssertEquals(expected, actual,true);
	}

	protected void jsonAssertEquals(JsonNode expected, String actual, Boolean strict) throws Exception {
		jsonAssertEquals(objectMapper.writeValueAsString(expected), actual, strict);
	}

	protected void jsonAssertEquals(String expected, JsonNode actual) throws Exception {
		jsonAssertEquals(expected, actual,true);
	}

	protected void jsonAssertEquals(String expected, JsonNode actual, Boolean strict) throws Exception {
		jsonAssertEquals(expected, objectMapper.writeValueAsString(actual), strict);
	}

	protected void jsonAssertEquals(String expected, JsonNode actual, JSONComparator comparator) throws Exception {
		jsonAssertEquals(expected, objectMapper.writeValueAsString(actual), comparator);
	}

	protected void jsonAssertEquals(String expected, String actual, JSONComparator comparator) throws Exception {
		JSONAssert.assertEquals(expected, actual, comparator);
	}

	protected void jsonAssertEquals(String expected, String actual) throws Exception {
		JSONAssert.assertEquals(expected, actual, true);
	}

	protected void jsonAssertEquals(String expected, String actual, Boolean strict) throws Exception {
		JSONAssert.assertEquals(expected, actual, strict);
	}

	protected void assertFlowEquals(Flow expected, Flow actual) {
		assertEquals(expected.getName(), actual.getName());
		assertEquals(expected.getBatchSize(), actual.getBatchSize());
		assertEquals(expected.getDescription(), actual.getDescription());
		assertEquals(expected.getThreadCount(), actual.getThreadCount());
		Map<String, Step> expectedSteps = expected.getSteps();
		Map<String, Step> actualSteps = expected.getSteps();
		assertEquals(expectedSteps.size(), actualSteps.size());
		for (Map.Entry<String, Step> expectedEntry:expectedSteps.entrySet()) {
			Step expectedStep = expectedEntry.getValue();
			Step actualStep = actualSteps.get(expectedEntry.getKey());
			String expectedStepId = expectedStep.getStepId() != null ? expectedStep.getStepId(): expectedStep.getName() + expectedStep.getStepDefinitionType().toString().toLowerCase();
			String actualStepId = actualStep.getStepId() != null ? actualStep.getStepId(): actualStep.getName() + actualStep.getStepDefinitionType().toString().toLowerCase();
			assertEquals(expectedStepId, actualStepId);
			// TODO more in-depth step level comparison which are now stored external to the flow
		}
	}

	protected ObjectNode readJsonObject(String json) {
		try {
			return (ObjectNode) objectMapper.readTree(json);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected ObjectNode readJsonObject(File json) {
		try {
			return (ObjectNode) objectMapper.readTree(json);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected ObjectNode readJsonObject(InputStream json) {
		try {
			return (ObjectNode) objectMapper.readTree(json);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected ArrayNode readJsonArray(String json) {
		try {
			return (ArrayNode) objectMapper.readTree(json);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static class InstallTracker {
		protected static boolean installedModules = false;
		protected static String lastMultiTenantValue = "";

		public static String getLastMultiTenantValue() {
			return lastMultiTenantValue;
		}

		public static void setLastMultiTenantValue(String lastMultiTenantValue) {
			InstallTracker.lastMultiTenantValue = lastMultiTenantValue;
		}

		public static boolean hasInstalledModules() {
			return installedModules;
		}

		public static void setInstalledModules(boolean installedModules) {
			InstallTracker.installedModules = installedModules;
		}

	}
}
