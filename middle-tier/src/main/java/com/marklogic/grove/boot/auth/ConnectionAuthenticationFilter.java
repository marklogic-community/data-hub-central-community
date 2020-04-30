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
import com.marklogic.hub.HubProject;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.envision.services.DeployService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@PropertySource({"classpath:application.properties"})
public class ConnectionAuthenticationFilter extends
    AbstractAuthenticationProcessingFilter {

    public Authentication authAttempt = null;

    @Autowired
    private HubConfigImpl hubConfig;

    @Autowired
	private DeployService deployService;

    @Autowired
	private HubProject hubProject;

	@Value("${dhfDir}")
	private File dhfDir;

	@Value("${dhfEnv}")
	private String dhfEnv;


    public ConnectionAuthenticationFilter() {
        super(new AntPathRequestMatcher("/api/auth/login", "POST"));
    }

    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException, IOException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException(
                "Authentication method not supported: " + request.getMethod());
        }

        final LoginInfo loginInfo = new ObjectMapper().readValue(request.getInputStream(), LoginInfo.class);
        String username = loginInfo.username;
        String password = loginInfo.password;

        if (username == null) {
            username = "";
        }

        if (password == null) {
            password = "";
        }

        username = username.trim();

		System.out.println("dhfDir: " + dhfDir.getCanonicalPath());
        String projectDir = dhfDir.getCanonicalPath();
        hubProject.createProject(projectDir);
        hubConfig.setMlUsername(username);
        hubConfig.setMlPassword(password);
        hubConfig.resetAppConfigs();
		String envName = dhfEnv;
        if (envName == null || envName.isEmpty()) {
            envName = "local";
		}
		System.out.println("envName: " + envName);
        hubConfig.withPropertiesFromEnvironment(envName);
        hubConfig.resetHubConfigs();
        hubConfig.refreshProject();
        hubConfig.getAppConfig().setAppServicesUsername(username);
        hubConfig.getAppConfig().setAppServicesPassword(password);

        ConnectionAuthenticationToken authRequest = new ConnectionAuthenticationToken(
            username, password, hubConfig.getAppConfig().getHost(), loginInfo.projectId, loginInfo.environment);

        // Allow subclasses to set the "details" property
        setDetails(request, authRequest);
        authAttempt = this.getAuthenticationManager().authenticate(authRequest);
        return authAttempt;
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
                              ConnectionAuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
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

        boolean needsInstall = deployService.needsInstall();
        response.getWriter().write("{\"needsInstall\":" + needsInstall + "}");
    }
}
