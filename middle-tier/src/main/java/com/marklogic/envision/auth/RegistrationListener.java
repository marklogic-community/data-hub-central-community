package com.marklogic.envision.auth;

import com.marklogic.envision.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class RegistrationListener implements
	ApplicationListener<OnRegistrationCompleteEvent> {

	private final EmailService emailService;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

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

		String subject = "Envision Registration Confirmation";
		String confirmationUrl = event.getAppUrl() + "/registrationConfirm?token=" + user.token;
		String body = "" + confirmationUrl;
		emailService.sendEmail(user.email, subject, body);
	}
}
