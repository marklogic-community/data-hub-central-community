package com.marklogic.envision.auth;

import org.springframework.context.ApplicationEvent;

public class OnRegistrationCompleteEvent extends ApplicationEvent {
	public String getAppUrl() {
		return appUrl;
	}

	public UserPojo getUser() {
		return user;
	}

	private final String appUrl;
	private final UserPojo user;

	public OnRegistrationCompleteEvent(
		UserPojo user, String appUrl) {
		super(user);

		this.user = user;
		this.appUrl = appUrl;
	}
}
