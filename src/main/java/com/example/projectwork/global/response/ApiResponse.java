package com.example.projectwork.global.response;

/**
 * 공통 응답 포맷 {@code {message, data}}. 실패 시 data는 null이다.
 */
public record ApiResponse<T>(String message, T data) {

	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(message, data);
	}

	public static ApiResponse<Void> error(String message) {
		return new ApiResponse<>(message, null);
	}
}
