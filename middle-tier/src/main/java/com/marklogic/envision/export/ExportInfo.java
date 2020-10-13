package com.marklogic.envision.export;

import java.util.Date;

public class ExportInfo {
	public String id;
	public Date creationDate;

	public ExportInfo(ExportPojo pojo) {
		this.id = pojo.id;
		this.creationDate = pojo.creationDate;
	}
}
