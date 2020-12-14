package com.marklogic.envision.upload;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public class UploadFile {
	private InputStream inputStream;
	private String fileName;

	public InputStream getInputStream() {
		return inputStream;
	}

	public String getFileName() {
		return fileName;
	}

	public UploadFile() {}

	public UploadFile(String fileName, InputStream inputStream) {
		this.fileName = fileName;
		this.inputStream = inputStream;
	}

	public UploadFile(MultipartFile file) {
		try {
			this.fileName = file.getOriginalFilename();
			this.inputStream = file.getInputStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
