package com.marklogic.envision.deploy;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.AppDeployer;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.envision.commands.DeployEntitiesCommand;
import com.marklogic.envision.commands.DeployEnvisionModulesCommand;
import com.marklogic.envision.config.EnvisionConfig;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.mgmt.ManageClient;
import com.marklogic.mgmt.resource.security.AmpManager;
import com.marklogic.mgmt.resource.security.RoleManager;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

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

	private void createAmps(ManageClient manageClient, String modulesDbName) {
		try {
			AmpManager ampManager = new AmpManager(manageClient);
			String amp = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("envision-config/amps/get-search-config.json"));
			amp = amp.replace("%%mlModulesDbName%%", modulesDbName);
			ampManager.save(amp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createEnvisionRole(ManageClient manageClient) {
		try {
			RoleManager roleManager = new RoleManager(manageClient);
			String role = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("envision-config/roles/envision.json"));
			roleManager.save(role);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void deploy() {
		ManageClient manageClient = config.getManageClient();
		AppDeployer appDeployer = new SimpleAppDeployer(manageClient, config.getAdminManager(), new DeployEnvisionModulesCommand(hubConfig));
		AppConfig appConfig = hubConfig.getAppConfig();
		appConfig.getCustomTokens().put("%%envisionVersion%%", config.getVersion());
		appDeployer.deploy(hubConfig.getAppConfig());
		createAmps(manageClient, appConfig.getModulesDatabaseName());
		createEnvisionRole(manageClient);
	}
}
