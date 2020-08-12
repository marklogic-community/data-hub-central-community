package com.marklogic.envision.deploy;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.AppDeployer;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.envision.commands.DeployEntitiesCommand;
import com.marklogic.envision.commands.DeployEnvisionModulesCommand;
import com.marklogic.envision.config.EnvisionConfig;
import com.marklogic.hub.impl.HubConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class DeployService {

	private final HubConfigImpl hubConfig;
	private final EnvisionConfig config;

	@Autowired
	DeployService(HubConfigImpl hubConfig, EnvisionConfig config) {
		this.hubConfig = hubConfig;
		this.config = config;
	}

	public boolean needsInstall() {
		String installedVersion = config.getInstalledVersion(hubConfig.newModulesDbClient());
		return installedVersion == null || !installedVersion.equals(config.getVersion());
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
			AppConfig appConfig = hubConfig.getAppConfig();
			appConfig.getCustomTokens().put("%%envisionVersion%%", config.getVersion());
			appDeployer.deploy(hubConfig.getAppConfig());
		}
		catch (Error error) {
			error.printStackTrace();
		}

	}
}
