package com.marklogic.envision.pojo;

public class StatusMessage {
	public String key;
	public String message;
	public String error;
	public Integer percentComplete;

	public StatusMessage(String key) {
		this.key = key;
	}

	public static StatusMessage newStatus(String key) {
		return new StatusMessage(key);
	}

	public StatusMessage withMessage(String message) {
		this.message = message;
		return this;
	}

	public StatusMessage withPercentComplete(int percentComplete) {
		this.percentComplete = percentComplete;
		return this;
	}

	public StatusMessage withError(String error) {
		this.error = error;
		return this;
	}
}
