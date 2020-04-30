package com.marklogic.grove.boot.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Not Authenticated")
public class NotAuthenticatedException extends RuntimeException { }
