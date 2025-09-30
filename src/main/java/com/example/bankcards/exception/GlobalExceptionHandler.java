package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(MissingRequestHeaderException.class)
	public ProblemDetail handleException(Exception e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}
	
	@ExceptionHandler(ResponseStatusException.class)
	public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
		return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getBody().getDetail());
	}
	
	@ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
	public ProblemDetail handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getLocalizedMessage());
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, List<String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		List<String> errors = new ArrayList<>();
		Map<String, List<String>> response = new HashMap<>();
		
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String errorMessage = error.getDefaultMessage();
			String field = ((FieldError) error).getField();

			errors.add(String.format("%s: %s", field, errorMessage));
		});
		
		response.put("errors", errors);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}
}