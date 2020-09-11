package com.marklogic.envision.auth;

import com.marklogic.envision.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class RegistrationListener extends
	EmailListener<OnRegistrationCompleteEvent> {

	private final EmailService emailService;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${app.url}")
	public String appUrl;

	@Autowired
	public RegistrationListener(EmailService emailService) {
		this.emailService = emailService;
	}

	@Override
	public void onApplicationEvent(OnRegistrationCompleteEvent event) {
		this.confirmRegistration(event);
	}

	private void confirmRegistration(OnRegistrationCompleteEvent event) {
		UserPojo user = event.getUser();

		String subject = "Welcome to MarkLogic Envision - please verify your account";
		String confirmationUrl = appUrl + "/registrationConfirm?token=" + user.token;
		String txtBody = getResource("verify-email.txt").replace("%%VERIFY_URL%%", confirmationUrl);
		String htmlBody = getResource("verify-email.html").replace("%%VERIFY_URL%%", confirmationUrl);
		emailService.sendEmail(user.email, subject, txtBody, htmlBody);
	}
}
