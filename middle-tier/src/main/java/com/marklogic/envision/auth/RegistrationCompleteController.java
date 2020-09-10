package com.marklogic.envision.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/registrationConfirm")
public class RegistrationCompleteController {

	private final UserService userService;

	@Autowired
	RegistrationCompleteController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping()
	public ModelAndView confirmRegistration(HttpServletRequest request, @RequestParam("token") String token) throws UnsupportedEncodingException {
		String redirect = "redirect:http://" + request.getServerName() + ":" + request.getServerPort();
		ValidateTokenPojo vtp = userService.validateToken(token);
		if (vtp.valid) {
			redirect += "/registrationComplete?accountVerified=true";
		}
		else {
			redirect += "/registrationComplete?error=" + URLEncoder.encode(vtp.error, "UTF-8");
		}
		return new ModelAndView(redirect);
	}
}
