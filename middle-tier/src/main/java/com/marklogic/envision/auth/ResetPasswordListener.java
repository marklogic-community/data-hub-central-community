package com.marklogic.envision.auth;

import com.marklogic.envision.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ResetPasswordListener implements
	ApplicationListener<OnResetPasswordEvent> {

	private final EmailService emailService;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	public ResetPasswordListener(EmailService emailService) {
		this.emailService = emailService;
	}

	@Override
	public void onApplicationEvent(OnResetPasswordEvent event) {
		UserPojo user = event.getUser();
		String subject = "Reset Your Password";
		String confirmationUrl = event.getAppUrl() + "/updatePassword?token=" + user.resetToken;
		String body = "" + confirmationUrl;
		emailService.sendEmail(user.email, subject, body);
	}
}
