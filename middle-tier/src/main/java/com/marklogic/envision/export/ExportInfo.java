package com.marklogic.envision.export;

import java.util.Date;

public class ExportInfo {
	public String id;
	public String name;
	public Date creationDate;

	public ExportInfo(ExportPojo pojo) {
		this.id = pojo.id;
		this.name = pojo.name;
		this.creationDate = pojo.creationDate;
	}
}
