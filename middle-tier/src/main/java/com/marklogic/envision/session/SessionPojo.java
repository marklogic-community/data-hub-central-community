package com.marklogic.envision.session;

import com.marklogic.client.pojo.annotation.Id;

public class SessionPojo {
	@Id
	public String user;
	public String currentModel;
}
