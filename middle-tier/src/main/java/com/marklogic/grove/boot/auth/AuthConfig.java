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

import com.marklogic.envision.config.EnvisionConfig;
import com.marklogic.envision.session.SessionManager;
import com.marklogic.spring.http.RestConfig;
import com.marklogic.spring.http.SimpleRestConfig;
import com.marklogic.spring.security.context.SpringSecurityCredentialsProvider;
import org.apache.http.client.CredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Extends Spring Boot's default web security configuration class and hooks in MarkLogic-specific classes from
 * marklogic-spring-web. Feel free to customize as needed.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class AuthConfig extends WebSecurityConfigurerAdapter {
    /**
     * @return a config class with ML connection properties
     */
    @Bean
    public RestConfig restConfig() {
        return new SimpleRestConfig();
    }

    @Bean
    public CredentialsProvider credentialsProvider() {
        return new SpringSecurityCredentialsProvider();
    }

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

	private final EnvisionConfig envisionConfig;
	private final SessionManager sessionManager;

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/h2-console/**");
    }

	@Autowired
	AuthConfig(
		RestAuthenticationEntryPoint restAuthenticationEntryPoint,
		EnvisionConfig envisionConfig,
		SessionManager sessionManager
	) {
    	this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    	this.envisionConfig = envisionConfig;
    	this.sessionManager = sessionManager;
	}

    /**
     * Configures what requests require authentication and which ones are always permitted. Uses CorsRequestMatcher to
     * allow for certain requests - e.g. put/post/delete requests - to be proxied successfully back to MarkLogic.
     *
     * This uses a form login by default, as for many MarkLogic apps (particularly demos), it's convenient to be able to
     * easily logout and login as a different user to show off security features. Spring Security has a very plain form
     * login page - you can customize this, just google for examples.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .exceptionHandling().authenticationEntryPoint(restAuthenticationEntryPoint)
            .and().cors()
			.and().headers().frameOptions().disable()
            .and().csrf().disable().authorizeRequests()
            .antMatchers(getAlwaysPermittedPatterns()).permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(new ConnectionAuthenticationFilter(envisionConfig, sessionManager), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JwtAuthorizationFilter(authenticationManager(), sessionManager), UsernamePasswordAuthenticationFilter.class)
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    /**
     * Defines a set of URLs that are always permitted - these are based on the presumed contents of the
     * src/main/resources/static directory.
     */
    protected String[] getAlwaysPermittedPatterns() {
        return new String[] {
            "/websocket/**",
            "/api/auth/login",
			"/api/auth/signup",
			"/api/auth/userExists",
			"/api/auth/resetPassword",
			"/api/auth/validateResetToken",
			"/api/auth/updatePassword",
			"/registrationConfirm",
			"/registrationComplete",
			"/updatePassword",
            "/",
            "/**/*.js",
            "/**/*.ttf",
            "/**/*.woff",
            "/**/*.svg",
            "/**/*.woff2",
			"/**/*.otf",
            "/**/*.eot",
            "/**/*.css",
			"/components/**",
            "/index.html",
            "/login",
			"/signup",
			"/userExists",
			"/upload",
			"/integrate/**",
			"/model",
			"/export",
            "/explore",
			"/explore/compare",
            "/know",
			"/detail",
			"/notifications",
			"/notifications/compare",
			"/admin",
			"/hostedadmin",
            "/404",
            "/assets/**",
            "/static/**",
            "/shutdown",
            "/img/**",
            "/**/*.ico",
            "/**/manifest.json"
        };
    }
}
