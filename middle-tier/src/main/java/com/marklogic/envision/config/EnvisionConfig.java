package com.marklogic.envision.config;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.eval.EvalResultIterator;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class EnvisionConfig {
	public String getInstalledVersion(DatabaseClient client) {
		try {
			EvalResultIterator result = client.newServerEval().javascript("require('/envision/config.sjs').version").eval();
			if (result.hasNext()) {
				return result.next().getString();
			}
		}
		catch(FailedRequestException e) {}
		return null;
	}

	public String getVersion() {
		Properties properties = new Properties();
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("version.properties")) {
			properties.load(inputStream);
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		String version = (String)properties.get("version");

		// this lets debug builds work from an IDE
		if (version.equals("${project.version}")) {
			version = "1.0.4";
		}
		return version;
	}
}
