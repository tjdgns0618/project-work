package com.example.projectwork.global.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.projectwork.global.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/** 비즈니스 규칙 위반 → ErrorCode의 상태코드. */
	@ExceptionHandler(ServiceException.class)
	public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException e) {
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity.status(errorCode.getHttpStatus())
				.body(ApiResponse.error(errorCode.getMessage()));
	}

	/** DB 제약 위반(예: 이메일 unique 경합) → 409. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.error(GlobalErrorCode.DATA_CONFLICT.getMessage()));
	}

	/** 예상하지 못한 예외 → 500 (표준 MVC 예외는 아래 handleExceptionInternal이 담당). */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error(GlobalErrorCode.INTERNAL_ERROR.getMessage()));
	}

	/**
	 * ResponseEntityExceptionHandler가 처리하는 표준 MVC 예외(깨진 JSON·미지원 메서드·타입 불일치 등)의
	 * 응답 본문을 공통 포맷 {@code {message, data}}으로 감싼다. 상태코드는 프레임워크가 정한 값을 그대로 사용한다.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		return super.handleExceptionInternal(
				ex, ApiResponse.error(messageFor(ex, statusCode)), headers, statusCode, request);
	}

	private String messageFor(Exception ex, HttpStatusCode statusCode) {
		if (ex instanceof MethodArgumentNotValidException validationEx) {
			var fieldError = validationEx.getBindingResult().getFieldError();
			if (fieldError != null && fieldError.getDefaultMessage() != null) {
				return fieldError.getDefaultMessage();
			}
			return GlobalErrorCode.INVALID_INPUT.getMessage();
		}
		if (statusCode.is4xxClientError()) {
			return GlobalErrorCode.INVALID_INPUT.getMessage();
		}
		return GlobalErrorCode.INTERNAL_ERROR.getMessage();
	}
}
