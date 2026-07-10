package com.example.projectwork.domain.member.exception;

import org.springframework.http.HttpStatus;

import com.example.projectwork.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
	INSUFFICIENT_POINT(HttpStatus.CONFLICT, "포인트 잔액이 부족합니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
