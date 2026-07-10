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
	public static OrderResponse from(Order order) {
		return new OrderResponse(
				order.getId(),
				order.getMember().getId(),
				order.getCoffee().getId(),
				order.getPayAmount(),
				order.getMember().getPointBalance(),
				order.getOrderedAt());
	}
}
