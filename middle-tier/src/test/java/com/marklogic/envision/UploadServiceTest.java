package com.marklogic.envision;

import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.envision.pojo.StatusMessage;
import com.marklogic.envision.upload.UploadFile;
import com.marklogic.envision.upload.UploadService;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.deploy.commands.LoadHubArtifactsCommand;
import com.marklogic.hub.deploy.commands.LoadHubModulesCommand;
import com.marklogic.hub.impl.HubConfigImpl;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.xml.sax.SAXException;
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
		installEnvisionModules();

		registerAccount();

		HubConfigImpl hubConfig = getHubConfig();
		SimpleAppDeployer deployer = new SimpleAppDeployer(hubConfig.getManageClient(), hubConfig.getAdminManager());
		deployer.getCommands().clear();
		deployer.getCommands().add(loadHubModulesCommand);
		deployer.getCommands().add(loadHubArtifactsCommand);
		deployer.deploy(hubConfig.getAppConfig());
	}

	@AfterEach
	void teardown() {
		clearStagingFinalAndJobDatabases();
		clearDatabases(HubConfig.DEFAULT_STAGING_SCHEMAS_DB_NAME, HubConfig.DEFAULT_FINAL_SCHEMAS_DB_NAME);
	}

	@Test
	void uploadCsv() throws Exception {
		assertEquals(0, getDocCount(getStagingClient(), "my-wacky-file.csv"));
		UploadFile uploadFile = new UploadFile("my-wacky-file.csv", getResourceStream("data/my-wacky-file.csv"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "my-wacky-file.csv");
		assertEquals(4, getDocCount(getStagingClient(), "my-wacky-file.csv"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getCollectionDoc(getStagingClient(), "my-wacky-file.csv", "sue"));
	}

	@Test
	void uploadCsvWithSpaces() throws Exception {
		assertEquals(0, getDocCount(getStagingClient(), "my-wacky-file.csv"));
		UploadFile uploadFile = new UploadFile("my file with   spaces.csv", getResourceStream("data/my-wacky-file.csv"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "my-wacky-file.csv");
		assertEquals(4, getDocCount(getStagingClient(), "my-wacky-file.csv"));
		assertEquals(4, getDocCountFromUriPattern(getStagingClient(), "/ingest/bob.smith@marklogic.com/my-wacky-file.csv/my_file_with_spaces.csv/*"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getCollectionDoc(getStagingClient(), "my-wacky-file.csv", "sue"));
	}

	@Test
	void uploadPsv() throws Exception {
		assertEquals(0, getDocCount(getStagingClient(), "my-wacky-file.psv"));
		UploadFile uploadFile = new UploadFile("my-wacky-file.psv", getResourceStream("data/pipe-sep.psv"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "my-wacky-file.psv");
		assertEquals(4, getDocCount(getStagingClient(), "my-wacky-file.psv"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getCollectionDoc(getStagingClient(), "my-wacky-file.psv", "sue"));
	}

	@Test
	void uploadJson() throws Exception {
		assertEquals(0, getDocCount(getStagingClient(), "My JSON Collection"));
		UploadFile uploadFile = new UploadFile("jsonUpload.json", getResourceStream("data/jsonUpload.json"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "My JSON Collection");
		assertEquals(1, getDocCount(getStagingClient(), "My JSON Collection"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/My_JSON_Collection/jsonUpload.json"));
	}

	@Test
	void uploadXml() throws IOException, SAXException {
		assertEquals(0, getDocCount(getStagingClient(), "My Xml Collection>"));
		UploadFile uploadFile = new UploadFile("xmlUpload.xml", getResourceStream("data/xmlUpload.xml"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "My Xml Collection");
		assertEquals(1, getDocCount(getStagingClient(), "My Xml Collection"));
		assertThat(new WhitespaceStrippedSource(Input.from(getResource("output/xmlUpload.xml")).build()), isIdenticalTo(new WhitespaceStrippedSource(Input.from(getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/My_Xml_Collection/xmlUpload.xml")).build())));
	}

	@Test
	void uploadBinary() {
		assertEquals(0, getDocCount(getStagingClient(), "My Binary"));
		UploadFile uploadFile = new UploadFile("LoanApplicationFraud.png", getResourceStream("data/LoanApplicationFraud.png"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "My Binary");
		assertEquals(1, getDocCount(getStagingClient(), "My Binary"));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(getStagingClient(), "/ingest/bob.smith@marklogic.com/My_Binary/LoanApplicationFraud.png"));
	}

	@Test
	void uploadZip() throws Exception {
		assertEquals(0, getDocCount(getStagingClient(), "zipUpload.zip"));
		UploadFile uploadFile = new UploadFile("zipUpload.zip", getResourceStream("data/zipUpload.zip"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "zipUpload.zip");
		assertEquals(11, getDocCount(getStagingClient(), "zipUpload.zip"));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(getStagingClient(), "/ingest/bob.smith@marklogic.com/zipUpload.zip/my-dir-name/LoanApplicationFraud.png"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/zipUpload.zip/my-dir-name/stuff.zip/stuff/jsonUpload.json"));
		assertThat(new WhitespaceStrippedSource(Input.from(getResource("output/xmlUpload.xml")).build()), isIdenticalTo(new WhitespaceStrippedSource(Input.from(getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/zipUpload.zip/my-dir-name/xmlUpload.xml")).build())));
	}

	@Test
	void uploadMultiples() throws Exception {
		assertEquals(0, getDocCount(getStagingClient(), "Multiple Upload"));
		UploadFile[] uploadFiles = Arrays.array(
			new UploadFile("my-wacky-file.csv", getResourceStream("data/my-wacky-file.csv")),
			new UploadFile("my-wacky-file.psv", getResourceStream("data/pipe-sep.psv")),
			new UploadFile("jsonUpload.json", getResourceStream("data/jsonUpload.json")),
			new UploadFile("xmlUpload.xml", getResourceStream("data/xmlUpload.xml")),
			new UploadFile("LoanApplicationFraud.png", getResourceStream("data/LoanApplicationFraud.png")),
			new UploadFile("zipUpload.zip", getResourceStream("data/zipUpload.zip"))
		);
		uploadService.uploadFiles(getNonAdminHubClient(), uploadFiles, "Multiple Upload");
		assertEquals(22, getDocCount(getStagingClient(), "Multiple Upload"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple_Upload/jsonUpload.json"));
		assertThat(new WhitespaceStrippedSource(Input.from(getResource("output/xmlUpload.xml")).build()), isIdenticalTo(new WhitespaceStrippedSource(Input.from(getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple_Upload/xmlUpload.xml")).build())));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple_Upload/LoanApplicationFraud.png"));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple_Upload/zipUpload.zip/my-dir-name/LoanApplicationFraud.png"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple_Upload/zipUpload.zip/my-dir-name/stuff.zip/stuff/jsonUpload.json"));
		assertThat(new WhitespaceStrippedSource(Input.from(getResource("output/xmlUpload.xml")).build()), isIdenticalTo(new WhitespaceStrippedSource(Input.from(getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple_Upload/zipUpload.zip/my-dir-name/xmlUpload.xml")).build())));
	}

	@Test
	void failedUpload() {
		clearStagingFinalAndJobDatabases();
		assertEquals(0, getDocCount(getStagingClient(), "my-wacky-file.csv"));
		UploadFile uploadFile = new UploadFile("my-wacky-file.csv", getResourceStream("data/my-wacky-file.csv"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "my-wacky-file.csv");
		assertEquals(0, getDocCount(getStagingClient(), "my-wacky-file.csv"));
		ArgumentCaptor<StatusMessage> argumentCaptor = ArgumentCaptor.forClass(StatusMessage.class);
		verify(template, atLeast(1)).convertAndSend(anyString(), argumentCaptor.capture());

		StatusMessage msg = argumentCaptor.getValue();
		assertNotNull(msg.error);
	}
}
