package com.example.projectwork.domain.coffee.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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

	private static final String MESSAGE =
			"{\"orderId\":10,\"memberId\":1,\"coffeeId\":2,\"payAmount\":4000,\"orderedAt\":\"2026-07-10T14:00:00\"}";

	private OrderCompletedEvent event() {
		return new OrderCompletedEvent(10L, 1L, 2L, 4000, LocalDateTime.of(2026, 7, 10, 14, 0));
	}

	@Test
	void 처음_처리하는_주문이면_해당_메뉴의_주문일_랭킹을_증가시킨다() {
		// given
		given(objectMapper.readValue(MESSAGE, OrderCompletedEvent.class)).willReturn(event());
		given(popularMenuRanking.markProcessed(10L)).willReturn(true);

		// when
		consumer.consume(MESSAGE);

		// then
		verify(popularMenuRanking).increment(2L, LocalDate.of(2026, 7, 10));
	}

	@Test
	void 재전달된_중복_주문이면_랭킹을_증가시키지_않는다() {
		// given — 같은 주문ID가 이미 처리됨(markProcessed=false)
		given(objectMapper.readValue(MESSAGE, OrderCompletedEvent.class)).willReturn(event());
		given(popularMenuRanking.markProcessed(10L)).willReturn(false);

		// when
		consumer.consume(MESSAGE);

		// then — 중복 집계 없음
		verify(popularMenuRanking, never()).increment(any(), any());
	}
}
