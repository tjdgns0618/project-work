package com.example.projectwork.domain.coffee.consumer;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.projectwork.domain.coffee.ranking.PopularMenuRanking;
import com.example.projectwork.domain.order.event.OrderCompletedEvent;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderCompletedConsumerTest {

	@Mock
	private PopularMenuRanking popularMenuRanking;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private OrderCompletedConsumer consumer;

	@Test
	void 주문_이벤트를_받으면_해당_메뉴의_주문일_랭킹을_증가시킨다() {
		// given
		String message = "{\"memberId\":1,\"coffeeId\":2,\"payAmount\":4000,\"orderedAt\":\"2026-07-10T14:00:00\"}";
		OrderCompletedEvent event = new OrderCompletedEvent(1L, 2L, 4000, LocalDateTime.of(2026, 7, 10, 14, 0));
		given(objectMapper.readValue(message, OrderCompletedEvent.class)).willReturn(event);

		// when
		consumer.consume(message);

		// then
		verify(popularMenuRanking).increment(2L, LocalDate.of(2026, 7, 10));
	}
}
