package com.marklogic.envision.auth;

import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.io.InputStream;

public class EmailListener<E extends ApplicationEvent> implements ApplicationListener<E> {

	protected InputStream getResourceStream(String resourceName) {
		return EmailListener.class.getClassLoader().getResourceAsStream(resourceName);
	}

	protected String getResource(String resourceName) {
		InputStream inputStream = null;
		String output;
		try {
			inputStream = getResourceStream(resourceName);
			output = IOUtils.toString(inputStream);
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
		return output;
	}

	@Override
	public void onApplicationEvent(E event) {

	}
}
