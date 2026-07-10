package com.example.projectwork.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberCreateRequest(

		@NotBlank
		@Email
		String email,

		@NotBlank
		String password,

		@NotBlank
		String name
) {
}
