package com.marklogic.envision.export;

import java.util.Date;
import java.util.UUID;

public class ExportPojo {
	public String id;
	public String zipUri;
	public Date creationDate;
	public String username;

	public ExportPojo() {}

	public ExportPojo(String zipUri, String username) {
		this.id = UUID.randomUUID().toString();
		this.username = username;
		this.zipUri = zipUri;
		this.creationDate = new Date();
	}
}
