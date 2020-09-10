/*
 * Copyright 2012-2019 MarkLogic Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.marklogic.grove.boot.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.envision.config.EnvisionConfig;
import com.marklogic.envision.dataServices.Users;
import com.marklogic.envision.hub.HubClient;
import com.marklogic.envision.session.SessionManager;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@PropertySource({"classpath:application.properties"})
public class ConnectionAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final EnvisionConfig envisionConfig;
	private final SessionManager sessionManager;

    public ConnectionAuthenticationFilter(EnvisionConfig envisionConfig, SessionManager sessionManager) {
        super(new AntPathRequestMatcher("/api/auth/login", "POST"));
        this.envisionConfig = envisionConfig;
		this.sessionManager = sessionManager;
		setAuthenticationFailureHandler(new LoginFailureHandler());
    }

    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException, IOException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException(
                "Authentication method not supported: " + request.getMethod());
        }

        final LoginInfo loginInfo = new ObjectMapper().readValue(request.getInputStream(), LoginInfo.class);
		AuthenticationToken token = authenticateUser(loginInfo.username, loginInfo.password);
		token.setDetails(authenticationDetailsSource.buildDetails(request));
		return token;
    }

    /**
     * Provided so that subclasses may configure what is put into the authentication
     * request's details property.
     *
     * @param request that an authentication request is being created for
     * @param authRequest the authentication request object that should have its details
     * set
     */
    protected void setDetails(HttpServletRequest request,
                              AuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }

	/**
	 * Authenticates the user and builds an AuthenticationToken containing the granted authorities.
	 * <p>
	 * This is the preferred method for determining if an authenticated user is permitted to perform a particular
	 * action. The SecurityService endpoint is expected to return an array of strings, each corresponding to a
	 * particular action. A Spring Security GrantedAuthority is constructed for each one, using "ROLE_" as a prefix,
	 * which is expected by Spring Security's default voting mechanism for whether a user can perform an action or not
	 * - see https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#appendix-faq-role-prefix.
	 *
	 * @param username - the username
	 * @param password - the password
	 */
	protected AuthenticationToken authenticateUser(String username, String password) {
		if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
			throw new BadCredentialsException("Unauthorized");
		}

		username = username.trim();

		HubClient hubClient = envisionConfig.newHubClient(username, password);
		sessionManager.setHubClient(username, hubClient);

		try {
			if (Users.on(sessionManager.getHubClient(username).getStagingClient()).testLogin()) {
				List<GrantedAuthority> authorities = new ArrayList<>();
				return new AuthenticationToken(username, password, authorities);
			}
		} catch (Exception e) {
//			e.printStackTrace();
		}
		throw new BadCredentialsException("Unauthorized");

	}

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) {
        String user = (String)authResult.getPrincipal();

        byte[] signingKey = SecurityConstants.JWT_SECRET.getBytes();

        List<String> roles = new ArrayList<>();
        roles.add("admin");

        String token = Jwts.builder()
                .setSubject(user)
                .signWith(SignatureAlgorithm.HS512, signingKey)
                .setHeaderParam("typ", SecurityConstants.TOKEN_TYPE)
                .setExpiration(new Date(System.currentTimeMillis() + 864000000))
                .claim("rol",roles)
                .compact();

        response.addHeader(SecurityConstants.TOKEN_HEADER, SecurityConstants.TOKEN_PREFIX + token);
    }
}
