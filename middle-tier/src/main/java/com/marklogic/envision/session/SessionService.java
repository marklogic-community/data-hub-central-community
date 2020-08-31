package com.marklogic.envision.session;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ResourceNotFoundException;
import com.marklogic.client.pojo.PojoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

	private String getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication.getName();
	}

	public SessionPojo getSession(DatabaseClient client) {
		String user = getCurrentUser();
		PojoRepository<SessionPojo, String> repo = client.newPojoRepository(SessionPojo.class, String.class);
		return repo.read(user);
	}

	public void saveSession(DatabaseClient client, SessionPojo session) {
		PojoRepository<SessionPojo, String> repo = client.newPojoRepository(SessionPojo.class, String.class);

		repo.write(session);
	}

	public void setCurrentModel(DatabaseClient client, String modelName) {
		SessionPojo sessionPojo;
		try {
			sessionPojo = getSession(client);
		}
		catch(ResourceNotFoundException e) {
			sessionPojo = new SessionPojo();
			sessionPojo.user = getCurrentUser();
		}
		sessionPojo.currentModel = modelName;
		saveSession(client, sessionPojo);
	}
}
