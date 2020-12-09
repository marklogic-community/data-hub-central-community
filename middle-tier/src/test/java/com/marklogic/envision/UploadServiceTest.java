package com.marklogic.envision;

import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.envision.pojo.StatusMessage;
import com.marklogic.envision.upload.UploadFile;
import com.marklogic.envision.upload.UploadService;
import com.marklogic.hub.deploy.commands.LoadHubArtifactsCommand;
import com.marklogic.hub.deploy.commands.LoadHubModulesCommand;
import com.marklogic.hub.impl.HubConfigImpl;
import org.assertj.core.util.Arrays;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

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
		XMLUnit.setIgnoreWhitespace(true);
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
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/My JSON Collection/jsonUpload.json"));
	}

	@Test
	void uploadXml() throws IOException, SAXException {
		assertEquals(0, getDocCount(getStagingClient(), "My Xml Collection>"));
		UploadFile uploadFile = new UploadFile("xmlUpload.xml", getResourceStream("data/xmlUpload.xml"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "My Xml Collection");
		assertEquals(1, getDocCount(getStagingClient(), "My Xml Collection"));
		XMLAssert.assertXMLEqual(getResource("output/xmlUpload.xml"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/My Xml Collection/xmlUpload.xml"));
	}

	@Test
	void uploadBinary() {
		assertEquals(0, getDocCount(getStagingClient(), "My Binary"));
		UploadFile uploadFile = new UploadFile("LoanApplicationFraud.png", getResourceStream("data/LoanApplicationFraud.png"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "My Binary");
		assertEquals(1, getDocCount(getStagingClient(), "My Binary"));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(getStagingClient(), "/ingest/bob.smith@marklogic.com/My Binary/LoanApplicationFraud.png"));
	}

	@Test
	void uploadZip() throws Exception {
		assertEquals(0, getDocCount(getStagingClient(), "zipUpload.zip"));
		UploadFile uploadFile = new UploadFile("zipUpload.zip", getResourceStream("data/zipUpload.zip"));
		uploadService.uploadFiles(getNonAdminHubClient(), Arrays.array(uploadFile), "zipUpload.zip");
		assertEquals(11, getDocCount(getStagingClient(), "zipUpload.zip"));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(getStagingClient(), "/ingest/bob.smith@marklogic.com/zipUpload.zip/my-dir-name/LoanApplicationFraud.png"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/zipUpload.zip/my-dir-name/stuff.zip/stuff/jsonUpload.json"));
		XMLAssert.assertXMLEqual(getResource("output/xmlUpload.xml"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/zipUpload.zip/my-dir-name/xmlUpload.xml"));
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
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple Upload/jsonUpload.json"));
		XMLAssert.assertXMLEqual(getResource("output/xmlUpload.xml"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple Upload/xmlUpload.xml"));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple Upload/LoanApplicationFraud.png"));
		assertArrayEquals(getResourceBytes("data/LoanApplicationFraud.png"), getDocumentBytes(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple Upload/zipUpload.zip/my-dir-name/LoanApplicationFraud.png"));
		jsonAssertEquals(getResource("output/jsonUpload.json"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple Upload/zipUpload.zip/my-dir-name/stuff.zip/stuff/jsonUpload.json"));
		XMLAssert.assertXMLEqual(getResource("output/xmlUpload.xml"), getDocumentString(getStagingClient(), "/ingest/bob.smith@marklogic.com/Multiple Upload/zipUpload.zip/my-dir-name/xmlUpload.xml"));
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
