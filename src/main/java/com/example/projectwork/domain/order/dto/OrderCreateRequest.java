package com.example.projectwork.domain.order.dto;

import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(

		@NotNull
		Long memberId,

		@NotNull
		Long coffeeId
) {
}
