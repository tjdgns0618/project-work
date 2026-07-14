package com.example.projectwork.domain.order.dto;

import java.time.LocalDateTime;

import com.example.projectwork.domain.order.entity.Order;

public record OrderResponse(
		Long orderId,
		Long memberId,
		Long coffeeId,
		int payAmount,
		Long pointBalance,
		LocalDateTime orderedAt
) {
	/** 차감 후 잔액은 원자 UPDATE 뒤 재조회한 값을 넘긴다. */
	public static OrderResponse from(Order order, long pointBalance) {
		return new OrderResponse(
				order.getId(),
				order.getMember().getId(),
				order.getCoffee().getId(),
				order.getPayAmount(),
				pointBalance,
				order.getOrderedAt());
	}
}
