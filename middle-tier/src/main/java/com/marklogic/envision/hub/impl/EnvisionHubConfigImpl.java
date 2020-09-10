package com.marklogic.envision.hub.impl;

import com.marklogic.hub.HubProject;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.mgmt.util.SimplePropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Properties;

public class EnvisionHubConfigImpl extends HubConfigImpl {

	public EnvisionHubConfigImpl(HubProject hubProject) {
		super(hubProject, new StandardEnvironment());
	}

	public void reset(Properties properties) {
		resetHubConfigs();
		applyProperties(new SimplePropertySource(properties));refreshProject();
		hydrateConfigs();
		hydrateAppConfigs(properties);
	}
}
