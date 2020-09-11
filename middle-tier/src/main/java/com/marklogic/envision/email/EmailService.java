package com.marklogic.envision.email;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

	private final EmailConfig emailConfig;

	@Autowired
	public EmailService(EmailConfig emailConfig) {
		this.emailConfig = emailConfig;
	}

	public void sendEmail(String recipient, String subject, String body) {
		try {
			HtmlEmail email = new HtmlEmail();
			email.setHostName(emailConfig.smtpHost);
			email.setSmtpPort(emailConfig.smtpPort);
			email.setAuthentication(emailConfig.smtpUsername, emailConfig.smtpPassword);
			email.setSSLOnConnect(emailConfig.useSSL);
			email.setFrom(emailConfig.fromEmail, emailConfig.fromName);
			email.addTo(recipient);
			email.setSubject(subject);
			email.setTextMsg(body);
			email.send();
		}
		catch(EmailException e) {
			e.printStackTrace();
		}
	}
}
