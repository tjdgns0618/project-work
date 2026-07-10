package com.example.projectwork.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.projectwork.global.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ServiceException.class)
	public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException e) {
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity.status(errorCode.getHttpStatus())
				.body(ApiResponse.error(errorCode.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldError() != null
				? e.getBindingResult().getFieldError().getDefaultMessage()
				: GlobalErrorCode.INVALID_INPUT.getMessage();
		return ResponseEntity.badRequest().body(ApiResponse.error(message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		return ResponseEntity.status(GlobalErrorCode.INTERNAL_ERROR.getHttpStatus())
				.body(ApiResponse.error(GlobalErrorCode.INTERNAL_ERROR.getMessage()));
	}
}
