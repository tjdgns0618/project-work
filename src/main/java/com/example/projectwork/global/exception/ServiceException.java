package com.example.projectwork.global.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 나타내는 예외. {@link ErrorCode}를 담아 던진다.
 */
@Getter
public class ServiceException extends RuntimeException {

	private final ErrorCode errorCode;

	public ServiceException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
