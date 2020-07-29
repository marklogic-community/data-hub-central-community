package com.marklogic.grove.boot.auth;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.HubConfig;
import com.marklogic.envision.deploy.DeployService;
import com.marklogic.hub.impl.HubConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is intended for development only, as it simply records a user as being "logged in" by virtue of being able to
 * instantiate a DatabaseClient, thereby assuming that the login credentials correspond to a MarkLogic user.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController extends AbstractController {

	final private DeployService deployService;

	@Autowired
	AuthController(HubConfigImpl hubConfig, DeployService deployService) {
		super(hubConfig);
		this.deployService = deployService;
	}

	@RequestMapping(value = "/status", method = RequestMethod.GET)
	public SessionStatus status() {
		HubConfig hubConfig = getHubConfig();
		String username = null;
		if (hubConfig != null) {
			AppConfig appConfig = hubConfig.getAppConfig();
			if (appConfig != null) {
				username = hubConfig.getAppConfig().getAppServicesUsername();
			}
		}
		boolean authenticated = username != null;
		boolean needsInstall = false;
		if (authenticated) {
			needsInstall = deployService.needsInstall();
		}
		return new SessionStatus(username, authenticated, needsInstall);
	}

	@RequestMapping(value = "/install", method = RequestMethod.POST)
	public void install() {
		deployService.deploy();
	}

	@RequestMapping(value = "/profile", method = RequestMethod.GET)
	public UserProfile profile() {
		UserProfile p = new UserProfile();
		p.setUsername(getHubConfig().getAppConfig().getAppServicesUsername());
		return p;
	}
}
