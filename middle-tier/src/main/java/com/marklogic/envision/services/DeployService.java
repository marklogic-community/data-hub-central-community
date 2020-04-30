package com.marklogic.envision.services;

import com.marklogic.appdeployer.AppDeployer;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.envision.commands.DeployEnvisionModulesCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeployService {

	@Autowired
	private HubConfigImpl hubConfig;

	public boolean needsInstall() {
		return hubConfig.getAppConfig().newModulesDatabaseClient().newDocumentManager().exists("/entities/search/findEntities.sjs") == null;
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
