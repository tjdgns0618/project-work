package com.example.projectwork.domain.order.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.example.projectwork.domain.order.event.OrderCompletedEvent;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

	private static final String TOPIC = "order.completed";

	@Mock
	private KafkaTemplate<Object, Object> kafkaTemplate;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private OrderEventListener orderEventListener;

	private OrderCompletedEvent event() {
		return new OrderCompletedEvent(10L, 1L, 2L, 4000, LocalDateTime.now());
	}

	@Test
	void 주문_이벤트를_JSON으로_order_completed_토픽에_발행한다() {
		// given — 주문ID·사용자 식별값·메뉴ID·결제금액이 담긴 이벤트
		OrderCompletedEvent event = event();
		String payload = "{\"orderId\":10,\"memberId\":1,\"coffeeId\":2,\"payAmount\":4000}";
		given(objectMapper.writeValueAsString(event)).willReturn(payload);
		CompletableFuture<SendResult<Object, Object>> ok = CompletableFuture.completedFuture(null);
		given(kafkaTemplate.send(TOPIC, payload)).willReturn(ok);

		// when
		orderEventListener.publish(event);

		// then
		verify(kafkaTemplate).send(TOPIC, payload);
	}

	@Test
	void 발행이_실패해도_예외를_전파하지_않는다() {
		// given — 브로커 장애로 발행 future가 실패
		OrderCompletedEvent event = event();
		String payload = "{\"orderId\":10}";
		given(objectMapper.writeValueAsString(event)).willReturn(payload);
		CompletableFuture<SendResult<Object, Object>> failed =
				CompletableFuture.failedFuture(new RuntimeException("broker down"));
		given(kafkaTemplate.send(TOPIC, payload)).willReturn(failed);

		// when & then — 결제 유지 정책: 발행 실패가 예외로 전파되지 않는다(로그만 남김)
		assertThatCode(() -> orderEventListener.publish(event)).doesNotThrowAnyException();
	}
}
