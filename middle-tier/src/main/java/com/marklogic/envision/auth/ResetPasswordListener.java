package com.marklogic.envision.auth;

import com.marklogic.envision.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ResetPasswordListener extends EmailListener<OnResetPasswordEvent> {

	private final EmailService emailService;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${app.url}")
	public String appUrl;

	@Autowired
	public ResetPasswordListener(EmailService emailService) {
		this.emailService = emailService;
	}

	@Override
	public void onApplicationEvent(OnResetPasswordEvent event) {
		UserPojo user = event.getUser();
		String subject = "Reset Your Password";
		String confirmationUrl = appUrl + "/updatePassword?token=" + user.resetToken;
		String txtBody = getResource("reset-password.txt").replace("%%PASSWORD_URL%%", confirmationUrl);
		String htmlBody = getResource("reset-password.html").replace("%%PASSWORD_URL%%", confirmationUrl);
		emailService.sendEmail(user.email, subject, txtBody, htmlBody);
	}
}
