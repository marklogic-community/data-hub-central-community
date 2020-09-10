package com.marklogic.grove.boot.auth;

import com.marklogic.grove.boot.AbstractController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

	@RequestMapping(value = "/status", method = RequestMethod.GET)
	public SessionStatus status() {
		String username = getCurrentUser();
		boolean authenticated = username != null;
		return new SessionStatus(username, authenticated);
	}
}
