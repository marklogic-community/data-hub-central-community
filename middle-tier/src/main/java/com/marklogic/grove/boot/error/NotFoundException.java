package com.marklogic.grove.boot.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "not found")
public class NotFoundException extends RuntimeException {
}
