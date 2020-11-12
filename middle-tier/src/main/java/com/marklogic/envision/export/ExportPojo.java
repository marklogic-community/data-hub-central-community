package com.marklogic.envision.export;

import java.util.Date;
import java.util.UUID;

public class ExportPojo {
	public String id;
	public String name;
	public String zipUri;

	public Date creationDate;
	public String username;

	public ExportPojo() {}

	public ExportPojo(String zipUri, String name, String username) {
		this.id = UUID.randomUUID().toString();
		this.name = name;
		this.username = username;
		this.zipUri = zipUri;
		this.creationDate = new Date();
	}
}
