package com.marklogic.envision;

import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.envision.pojo.StatusMessage;
import com.marklogic.envision.upload.UploadService;
import com.marklogic.hub.deploy.commands.LoadHubArtifactsCommand;
import com.marklogic.hub.deploy.commands.LoadHubModulesCommand;
import com.marklogic.hub.impl.HubConfigImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
	void setup() {
		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		HubConfigImpl hubConfig = getHubConfig();
		SimpleAppDeployer deployer = new SimpleAppDeployer(hubConfig.getManageClient(), hubConfig.getAdminManager());
		deployer.getCommands().clear();
		deployer.getCommands().add(loadHubModulesCommand);
		deployer.getCommands().add(loadHubArtifactsCommand);
		deployer.deploy(hubConfig.getAppConfig());
	}

	@Test
	void upload() {
		assertEquals(0, getDocCount(getStagingClient(), "my-wacky-file.csv"));
		uploadService.uploadFile(getNonAdminHubClient(), getResourceStream("data/my-wacky-file.csv"), "my-wacky-file.csv");
		assertEquals(4, getDocCount(getStagingClient(), "my-wacky-file.csv"));
	}

	@Test
	void uploadPsv() {
		assertEquals(0, getDocCount(getStagingClient(), "my-wacky-file.csv"));
		uploadService.uploadFile(getNonAdminHubClient(), getResourceStream("data/pipe-sep.psv"), "my-wacky-file.csv");
		assertEquals(4, getDocCount(getStagingClient(), "my-wacky-file.csv"));
	}

	@Test
	void failedUpload() {
		clearStagingFinalAndJobDatabases();
		assertEquals(0, getDocCount(getStagingClient(), "my-wacky-file.csv"));
		uploadService.uploadFile(getNonAdminHubClient(), getResourceStream("data/my-wacky-file.csv"), "my-wacky-file.csv");
		assertEquals(0, getDocCount(getStagingClient(), "my-wacky-file.csv"));
		ArgumentCaptor<StatusMessage> argumentCaptor = ArgumentCaptor.forClass(StatusMessage.class);
		verify(template, atLeast(1)).convertAndSend(anyString(), argumentCaptor.capture());

		StatusMessage msg = argumentCaptor.getValue();
		assertNotNull(msg.error);
	}
}
