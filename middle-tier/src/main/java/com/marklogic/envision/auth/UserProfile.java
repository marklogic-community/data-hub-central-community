package com.marklogic.envision.auth;

public class UserProfile {
	public String email;
	public String name;

	public UserProfile() {}

	public UserProfile(String name) {
		this.name = name;
	}
}
