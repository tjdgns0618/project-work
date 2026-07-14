package com.example.projectwork.domain.order.listener;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.projectwork.domain.order.event.OrderCompletedEvent;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

	@Mock
	private KafkaTemplate<Object, Object> kafkaTemplate;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private OrderEventListener orderEventListener;

	@Test
	void 주문_이벤트를_JSON으로_order_completed_토픽에_발행한다() {
		// given — 사용자 식별값·메뉴ID·결제금액이 담긴 이벤트
		OrderCompletedEvent event = new OrderCompletedEvent(1L, 2L, 4000, LocalDateTime.now());
		String payload = "{\"memberId\":1,\"coffeeId\":2,\"payAmount\":4000}";
		given(objectMapper.writeValueAsString(event)).willReturn(payload);

		// when
		orderEventListener.publish(event);

		// then
		verify(kafkaTemplate).send("order.completed", payload);
	}
}
