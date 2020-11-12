package com.marklogic.envision.session;

import com.marklogic.envision.hub.HubClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SessionManager {

	private final Map<String, HubClient> clients = new HashMap<>();

	public HubClient getHubClient(String username) {
		return clients.get(username);
	}

	public void setHubClient(String username, HubClient client) {
		clients.put(username, client);
	}
}
