package com.marklogic.grove.boot;

import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.session.SessionManager;
import com.marklogic.grove.boot.error.NotAuthenticatedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class AbstractController extends LoggingObject {

	@Autowired
	SessionManager sessionManager;

	protected String getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication.getName();
	}

	protected HubClient getHubClient() {
		HubClient client = sessionManager.getHubClient(getCurrentUser());
		if (client == null) {
			throw new NotAuthenticatedException();
		}
		return client;
	}
}
