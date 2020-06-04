package com.marklogic.envision.deploy;

import com.marklogic.appdeployer.AppDeployer;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.envision.commands.DeployEntitiesCommand;
import com.marklogic.hub.deploy.commands.LoadUserArtifactsCommand;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.envision.commands.DeployEnvisionModulesCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeployService {

	@Autowired
	private HubConfigImpl hubConfig;

	public boolean needsInstall() {
		return hubConfig.getAppConfig().newModulesDatabaseClient().newDocumentManager().exists("/envision/search/findEntities.sjs") == null;
	}

	public void deployEntities() {
		try {
			AppDeployer appDeployer = new SimpleAppDeployer(hubConfig.getManageClient(), hubConfig.getAdminManager(), new DeployEntitiesCommand(hubConfig));
			appDeployer.deploy(hubConfig.getAppConfig());
		}
		catch (Error error) {
			error.printStackTrace();
		}

	}
	public void deploy() {
		try {
			AppDeployer appDeployer = new SimpleAppDeployer(hubConfig.getManageClient(), hubConfig.getAdminManager(), new DeployEnvisionModulesCommand(hubConfig));
			appDeployer.deploy(hubConfig.getAppConfig());
		}
		catch (Error error) {
			error.printStackTrace();
		}

	}
}
