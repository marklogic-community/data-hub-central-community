package com.marklogic.envision.auth;

import org.springframework.context.ApplicationEvent;

public class OnRegistrationCompleteEvent extends ApplicationEvent {
	public UserPojo getUser() {
		return user;
	}

	private final UserPojo user;

	public OnRegistrationCompleteEvent(
		UserPojo user) {
		super(user);
		this.user = user;
	}
}
