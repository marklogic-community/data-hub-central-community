package com.marklogic.envision.auth;

import org.springframework.context.ApplicationEvent;

public class OnResetPasswordEvent extends ApplicationEvent {
	public UserPojo getUser() {
		return user;
	}

	private final UserPojo user;

	public OnResetPasswordEvent(
		UserPojo user) {
		super(user);
		this.user = user;
	}
}
