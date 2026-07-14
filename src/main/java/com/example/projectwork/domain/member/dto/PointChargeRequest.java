package com.example.projectwork.domain.member.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PointChargeRequest(

		@NotNull
		@Positive
		Long amount
) {
}
