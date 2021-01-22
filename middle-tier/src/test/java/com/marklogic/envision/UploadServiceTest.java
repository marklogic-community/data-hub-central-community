package com.marklogic.envision;

import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.pojo.StatusMessage;
import com.marklogic.envision.upload.UploadFile;
import com.marklogic.envision.upload.UploadService;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.deploy.commands.LoadHubArtifactsCommand;
import com.marklogic.hub.deploy.commands.LoadHubModulesCommand;
import com.marklogic.hub.impl.HubConfigImpl;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.xmlunit.builder.Input;
import org.xmlunit.input.WhitespaceStrippedSource;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

public class UploadServiceTest extends BaseTest {

	@MockBean
	SimpMessagingTemplate template;

	@Autowired
	UploadService uploadService;

	@Autowired
	private LoadHubModulesCommand loadHubModulesCommand;

	@Autowired
	private LoadHubArtifactsCommand loadHubArtifactsCommand;

	@BeforeEach
	void setup() throws IOException {
		removeUser(ACCOUNT_NAME);
		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_STAGING_SCHEMAS_DB_NAME, HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME);

		HubConfigImpl hubConfig = getHubConfig();
		SimpleAppDeployer deployer = new SimpleAppDeployer(hubConfig.getManageClient(), hubConfig.getAdminManager());
		deployer.getCommands().clear();
		deployer.getCommands().add(loadHubModulesCommand);
		deployer.getCommands().add(loadHubArtifactsCommand);
		deployer.deploy(hubConfig.getAppConfig());

		installEnvisionModules();

		registerAccount();
	}

//	@AfterEach
//	void teardown() {
//		clearStagingFinalAndJobDatabases();
//		clearDatabases(HubConfig.DEFAULT_STAGING_SCHEMAS_DB_NAME, HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME);
//	}

	private DatabaseClient getDbClient(String database) {
		if (database.equals("staging")) {
			return getStagingClient();
		}
		return getFinalClient();
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadCsv(String database) throws Exception {
		DatabaseClient client = getDbClient(database);
		assertEquals(0, getDocCount(client, "my-wacky-file.csv"));
		UploadFile uploadFile = new UploadFile("my-wacky-file.csv", getResourceStream("data/my-wacky-file.csv"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), database, "my-wacky-file.csv");
		assertEquals(4, getDocCount(client, "my-wacky-file.csv"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getCollectionDoc(client, "my-wacky-file.csv", "sue"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadCsvWithSpaces(String database) throws Exception {
		DatabaseClient client = getDbClient(database);
		assertEquals(0, getDocCount(client, "my-wacky-file.csv"));
		UploadFile uploadFile = new UploadFile("my file with   spaces.csv", getResourceStream("data/my-wacky-file.csv"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), database, "my-wacky-file.csv");
		assertEquals(4, getDocCount(client, "my-wacky-file.csv"));
		assertEquals(4, getDocCountFromUriPattern(client, "/ingest/bob.smith@marklogic.com/my-wacky-file.csv/my_file_with_spaces.csv/*"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getCollectionDoc(client, "my-wacky-file.csv", "sue"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadPsv(String database) throws Exception {
		DatabaseClient client = getDbClient(database);
		assertEquals(0, getDocCount(client, "my-wacky-file.psv"));
		UploadFile uploadFile = new UploadFile("my-wacky-file.psv", getResourceStream("data/pipe-sep.psv"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), database, "my-wacky-file.psv");
		assertEquals(4, getDocCount(client, "my-wacky-file.psv"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getCollectionDoc(client, "my-wacky-file.psv", "sue"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadJson(String database) throws Exception {
		DatabaseClient client = getDbClient(database);
		assertEquals(0, getDocCount(client, "My JSON Collection"));
		UploadFile uploadFile = new UploadFile("jsonUpload.json", getResourceStream("data/jsonUpload.json"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), database, "My JSON Collection");
		assertEquals(1, getDocCount(client, "My JSON Collection"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(client, "/ingest/bob.smith@marklogic.com/My_JSON_Collection/jsonUpload.json"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadXml(String database) {
		DatabaseClient client = getDbClient(database);
		assertEquals(0, getDocCount(client, "My Xml Collection>"));
		UploadFile uploadFile = new UploadFile("xmlUpload.xml", getResourceStream("data/xmlUpload.xml"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), database, "My Xml Collection");
		assertEquals(1, getDocCount(client, "My Xml Collection"));
		assertThat(new WhitespaceStrippedSource(Input.from(getResource("output/xmlUpload.xml")).build()), isIdenticalTo(new WhitespaceStrippedSource(Input.from(getDocumentString(client, "/ingest/bob.smith@marklogic.com/My_Xml_Collection/xmlUpload.xml")).build())));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadBinary(String database) {
		DatabaseClient client = getDbClient(database);
		assertEquals(0, getDocCount(client, "My Binary"));
		UploadFile uploadFile = new UploadFile("LoanApplicationFraud.png", getResourceStream("data/LoanApplicationFraud.png"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), database, "My Binary");
		assertEquals(1, getDocCount(client, "My Binary"));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(client, "/ingest/bob.smith@marklogic.com/My_Binary/LoanApplicationFraud.png"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadZip(String database) throws Exception {
		DatabaseClient client = getDbClient(database);
		assertEquals(0, getDocCount(client, "zipUpload.zip"));
		UploadFile uploadFile = new UploadFile("zipUpload.zip", getResourceStream("data/zipUpload.zip"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), database, "zipUpload.zip");
		assertEquals(11, getDocCount(client, "zipUpload.zip"));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(client, "/ingest/bob.smith@marklogic.com/zipUpload.zip/my-dir-name/LoanApplicationFraud.png"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(client, "/ingest/bob.smith@marklogic.com/zipUpload.zip/my-dir-name/stuff.zip/stuff/jsonUpload.json"));
		assertThat(new WhitespaceStrippedSource(Input.from(getResource("output/xmlUpload.xml")).build()), isIdenticalTo(new WhitespaceStrippedSource(Input.from(getDocumentString(client, "/ingest/bob.smith@marklogic.com/zipUpload.zip/my-dir-name/xmlUpload.xml")).build())));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadMultiples(String database) throws Exception {
		DatabaseClient client = getDbClient(database);
		assertEquals(0, getDocCount(client, "Multiple Upload"));
		UploadFile[] uploadFiles = Arrays.array(
			new UploadFile("my-wacky-file.csv", getResourceStream("data/my-wacky-file.csv")),
			new UploadFile("my-wacky-file.psv", getResourceStream("data/pipe-sep.psv")),
			new UploadFile("jsonUpload.json", getResourceStream("data/jsonUpload.json")),
			new UploadFile("xmlUpload.xml", getResourceStream("data/xmlUpload.xml")),
			new UploadFile("LoanApplicationFraud.png", getResourceStream("data/LoanApplicationFraud.png")),
			new UploadFile("zipUpload.zip", getResourceStream("data/zipUpload.zip"))
		);
		uploadService.uploadFiles(getNonAdminHubClient(), uploadFiles, database, "Multiple Upload");
		assertEquals(22, getDocCount(client, "Multiple Upload"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(client, "/ingest/bob.smith@marklogic.com/Multiple_Upload/jsonUpload.json"));
		assertThat(new WhitespaceStrippedSource(Input.from(getResource("output/xmlUpload.xml")).build()), isIdenticalTo(new WhitespaceStrippedSource(Input.from(getDocumentString(client, "/ingest/bob.smith@marklogic.com/Multiple_Upload/xmlUpload.xml")).build())));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(client, "/ingest/bob.smith@marklogic.com/Multiple_Upload/LoanApplicationFraud.png"));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(client, "/ingest/bob.smith@marklogic.com/Multiple_Upload/zipUpload.zip/my-dir-name/LoanApplicationFraud.png"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(client, "/ingest/bob.smith@marklogic.com/Multiple_Upload/zipUpload.zip/my-dir-name/stuff.zip/stuff/jsonUpload.json"));
		assertThat(new WhitespaceStrippedSource(Input.from(getResource("output/xmlUpload.xml")).build()), isIdenticalTo(new WhitespaceStrippedSource(Input.from(getDocumentString(client, "/ingest/bob.smith@marklogic.com/Multiple_Upload/zipUpload.zip/my-dir-name/xmlUpload.xml")).build())));
	}

//	void uploadSemanticsJson() throws Exception {
//		assertEquals(0, getDocCount(client, "Multiple Upload"));
//		UploadFile[] uploadFiles = Arrays.array(
//			new UploadFile("test.json", getResourceStream("data/semantics/test.json"))
//		);
//		assertEquals(0, getTriplesCount(client));
//		uploadService.uploadFiles(getNonAdminHubClient(), uploadFiles, "Semantics Upload");
//		assertEquals(13257, getTriplesCount(client));
//	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadSemanticsN3(String database) {
		DatabaseClient client = getDbClient(database);
		UploadFile[] uploadFiles = Arrays.array(
			new UploadFile("test.n3", getResourceStream("data/semantics/test.n3"))
		);
		assertEquals(0, getTriplesCount(client));
		uploadService.uploadFiles(getNonAdminHubClient(), uploadFiles, database, "Semantics Upload");
		assertEquals(13257, getTriplesCount(client));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadSemanticsNQ(String database) {
		DatabaseClient client = getDbClient(database);
		UploadFile[] uploadFiles = Arrays.array(
			new UploadFile("test.nq", getResourceStream("data/semantics/test.nq"))
		);
		assertEquals(0, getTriplesCount(client));
		uploadService.uploadFiles(getNonAdminHubClient(), uploadFiles, database, "Semantics Upload");
		assertEquals(2001, getTriplesCount(client));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadSemanticsNt(String database) {
		DatabaseClient client = getDbClient(database);
		UploadFile[] uploadFiles = Arrays.array(
			new UploadFile("test.nt", getResourceStream("data/semantics/test.nt"))
		);
		assertEquals(0, getTriplesCount(client));
		uploadService.uploadFiles(getNonAdminHubClient(), uploadFiles, database, "Semantics Upload");
		assertEquals(3, getTriplesCount(client));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadSemanticsRdf(String database) {
		DatabaseClient client = getDbClient(database);
		UploadFile[] uploadFiles = Arrays.array(
			new UploadFile("test.rdf", getResourceStream("data/semantics/test.rdf"))
		);
		assertEquals(0, getTriplesCount(client));
		uploadService.uploadFiles(getNonAdminHubClient(), uploadFiles, database, "Semantics Upload");
		assertEquals(454, getTriplesCount(client));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadSemanticsTrig(String database) {
		DatabaseClient client = getDbClient(database);
		UploadFile[] uploadFiles = Arrays.array(
			new UploadFile("test.trig", getResourceStream("data/semantics/test.trig"))
		);
		assertEquals(0, getTriplesCount(client));
		uploadService.uploadFiles(getNonAdminHubClient(), uploadFiles, database, "Semantics Upload");
		assertEquals(16, getTriplesCount(client));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void uploadSemanticsTtl(String database) {
		DatabaseClient client = getDbClient(database);
		UploadFile[] uploadFiles = Arrays.array(
			new UploadFile("test.ttl", getResourceStream("data/semantics/test.ttl"))
		);
		assertEquals(0, getTriplesCount(client));
		uploadService.uploadFiles(getNonAdminHubClient(), uploadFiles, database, "Semantics Upload");
		assertEquals(795, getTriplesCount(client));
	}

	@ParameterizedTest
	@ValueSource(strings = {"staging", "final"})
	void failedUpload(String database) {
		DatabaseClient client = getDbClient(database);
		clearStagingFinalAndJobDatabases();
		assertEquals(0, getDocCount(client, "my-wacky-file.csv"));
		UploadFile uploadFile = new UploadFile("my-wacky-file.csv", getResourceStream("data/my-wacky-file.csv"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), database, "my-wacky-file.csv");
		assertEquals(0, getDocCount(client, "my-wacky-file.csv"));
		ArgumentCaptor<StatusMessage> argumentCaptor = ArgumentCaptor.forClass(StatusMessage.class);
		verify(template, atLeast(1)).convertAndSend(anyString(), argumentCaptor.capture());

		StatusMessage msg = argumentCaptor.getValue();
		assertNotNull(msg.error);
	}
}
