package com.marklogic.envision.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfig {
	@Value("${smtp.host:}")
	public String smtpHost;

	@Value("${smtp.port:}")
	public Integer smtpPort;

	@Value("${smtp.username:}")
	public String smtpUsername;

	@Value("${smtp.password:}")
	public String smtpPassword;

	@Value("${from.email:}")
	public String fromEmail;

	@Value("${from.name:}")
	public String fromName;

	@Value("${smtp.useSSL:false}")
	public boolean useSSL;
}
