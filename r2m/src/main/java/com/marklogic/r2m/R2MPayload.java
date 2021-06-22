package com.marklogic.r2m;

public class R2MPayload {
	private TableQuery query;
	private SourceConfiguration source;
	private MLInsertConfig insert;
	private MarkLogicConfiguration mlConfig;

	public TableQuery getQuery() {
		return query;
	}

	public void setQuery(TableQuery query) {
		this.query = query;
	}

	public SourceConfiguration getSource() {
		return source;
	}

	public void setSource(SourceConfiguration source) {
		this.source = source;
	}

	public MLInsertConfig getInsert() {
		return insert;
	}

	public void setInsert(MLInsertConfig insert) {
		this.insert = insert;
	}

	public MarkLogicConfiguration getMlConfig() {
		return mlConfig;
	}

	public void setMlConfig(MarkLogicConfiguration mlConfig) {
		this.mlConfig = mlConfig;
	}
}
