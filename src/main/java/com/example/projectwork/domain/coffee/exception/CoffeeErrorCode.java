package com.example.projectwork.domain.coffee.exception;

import org.springframework.http.HttpStatus;

import com.example.projectwork.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CoffeeErrorCode implements ErrorCode {

	COFFEE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 메뉴입니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
