package com.example.projectwork.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 계약. 실제 HttpStatus와 사용자 메시지를 갖는다.
 * 공통 에러는 {@link GlobalErrorCode}, 도메인 에러는 각 도메인의 exception 패키지에서 구현한다.
 */
public interface ErrorCode {

	HttpStatus getHttpStatus();

	String getMessage();
}
