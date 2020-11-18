package com.marklogic.envision;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class IndexController {
	/**
	 * Assumes that the root URL should use a template named "index", which presumably will setup the Angular app.
	 */
	@RequestMapping(value = {
		"/login",
		"/signup",
		"/forgotPassword",
		"/updatePassword",
		"/registrationComplete",
		"/model",
		"/explore",
		"/explore/compare",
		"/know",
		"/admin",
		"/hostedadmin",
		"/notifications",
		"/notifications/compare",
		"/detail",
		"/export",
		"/404",
		"/upload",
		"/integrate/**"
	}, method = RequestMethod.GET)
	public String index() {
		return "/";
	}
}
