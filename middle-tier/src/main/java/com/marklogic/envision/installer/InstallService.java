package com.marklogic.envision.installer;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.AppDeployer;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.commands.DeployEnvisionModulesCommand;
import com.marklogic.envision.config.EnvisionConfig;
import com.marklogic.mgmt.ManageClient;
import com.marklogic.mgmt.resource.security.AmpManager;
import com.marklogic.mgmt.resource.security.RoleManager;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Service
public class InstallService {
	private final EnvisionConfig config;

	@Autowired
	InstallService(EnvisionConfig config) {
		this.config = config;
	}

	private boolean needsInstall() {
		String installedVersion = config.getInstalledVersion();
		return installedVersion == null || !installedVersion.equals(config.getVersion());
	}

	public void install() {
		install(false);
	}

	public void install(boolean force) {
		if (!force && !needsInstall()) return;

		System.out.println("Installing Envision Modules...\n");

		ManageClient manageClient = config.getManageClient();
		AppDeployer appDeployer = new SimpleAppDeployer(manageClient, config.getAdminManager(), new DeployEnvisionModulesCommand(config.getModulesClient(), config.isMultiTenant()));
		AppConfig appConfig = config.getAdminHubConfig().getAppConfig();
		appConfig.getCustomTokens().put("%%envisionVersion%%", config.getVersion());
		appConfig.getCustomTokens().put("%%envisionIsMultiTenant%%", String.valueOf(config.isMultiTenant()));
		appDeployer.deploy(appConfig);
		createAmps(manageClient, appConfig.getModulesDatabaseName());
		createEnvisionRole(manageClient, config.getClient());
		createEnvisionAdminRole(manageClient);
		System.out.println("...Install Complete.\n");
	}

	private void createAmps(ManageClient manageClient, String modulesDbName) {
		try {
			String[] files = {
				"get-search-config.json",
				"resetSystem.json",
				"createTdes.json",
				"updatePii.json"
			};
			for (String file: files) {
				String amp = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("envision-config/amps/" + file)));
				AmpManager ampManager = new AmpManager(manageClient);
				amp = amp.replace("%%mlModulesDbName%%", modulesDbName);
				ampManager.save(amp);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createEnvisionRole(ManageClient manageClient, DatabaseClient client) {
		try {
			RoleManager roleManager = new RoleManager(manageClient);
			String role = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("envision-config/roles/envision.json")));
			roleManager.save(role);

			client.newServerEval().xquery("xquery version \"1.0-ml\";\n" +
				"import module namespace sec=\"http://marklogic.com/xdmp/security\" at \n" +
				"     \"/MarkLogic/security.xqy\";\n" +
				"\n" +
				"xdmp:invoke-function(function() {\n" +
				"  sec:collection-add-permissions(\n" +
				"    \"http://marklogic.com/xdmp/tde\",\n" +
				"    (xdmp:permission(\"envision\", \"read\"), xdmp:permission(\"envision\", \"update\"))\n" +
				"  )\n" +
				"}, map:entry(\"database\", xdmp:security-database()))").eval();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createEnvisionAdminRole(ManageClient manageClient) {
		try {
			RoleManager roleManager = new RoleManager(manageClient);
			String role = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("envision-config/roles/envisionAdmin.json")));
			roleManager.save(role);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
