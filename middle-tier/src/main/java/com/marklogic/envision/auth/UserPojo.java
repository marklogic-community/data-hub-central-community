package com.marklogic.envision.auth;

import com.marklogic.client.pojo.annotation.Id;

import java.util.Date;

public class UserPojo {
	@Id
	public String email;
	public String name;
	public String password;
	public String token;
	public Date tokenExpiry;
	public String resetToken;
	public Date resetTokenExpiry;
	public Boolean validated;

	public UserProfile toUserProfile() {
		UserProfile profile = new UserProfile();
		profile.email = email;
		profile.name = name;
		return profile;
	}
}

