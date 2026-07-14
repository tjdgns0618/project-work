package com.example.projectwork.domain.coffee.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.projectwork.domain.coffee.ranking.PopularMenuRanking;
import com.example.projectwork.domain.order.event.OrderCompletedEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * 주문 완료 이벤트를 소비해 인기 메뉴 Redis 랭킹에 적재한다.
 * Redis는 이 이벤트로 채워지는 materialized view이며, 원장(ORDER)이 정확성의 원천이다.
 */
@Component
@RequiredArgsConstructor
public class OrderCompletedConsumer {

	private final PopularMenuRanking popularMenuRanking;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "order.completed", groupId = "coffee-service")
	public void consume(String message) {
		OrderCompletedEvent event = objectMapper.readValue(message, OrderCompletedEvent.class);
		popularMenuRanking.increment(event.coffeeId(), event.orderedAt().toLocalDate());
	}
}
