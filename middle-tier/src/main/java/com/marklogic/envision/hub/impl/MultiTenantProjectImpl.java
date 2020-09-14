package com.marklogic.envision.hub.impl;

import com.marklogic.hub.HubProject;
import com.marklogic.hub.impl.HubProjectImpl;

import java.nio.file.Path;

public class MultiTenantProjectImpl extends HubProjectImpl {

	private final String username;

	public MultiTenantProjectImpl(HubProject hubProject, String username) {
		this.username = username;
		createProject(hubProject.getProjectDirString());
		this.setUserModulesDeployTimestampFile(hubProject.getUserModulesDeployTimestampFile());
	}

	@Override public Path getHubEntitiesDir() { return getProjectDir().resolve("entities").resolve(username); }

	@Override public Path getHubMappingsDir() {
		return getProjectDir().resolve("mappings").resolve(username);
	}
}
