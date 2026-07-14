package com.example.projectwork.domain.notification.consumer;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.projectwork.domain.notification.sender.NotificationSender;
import com.example.projectwork.domain.order.event.OrderCompletedEvent;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

	@Mock
	private NotificationSender notificationSender;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private NotificationConsumer consumer;

	@Test
	void 주문_이벤트를_받으면_해당_회원에게_주문정보가_담긴_알림을_발송한다() {
		// given
		String message = "{\"orderId\":10,\"memberId\":1,\"coffeeId\":2,\"payAmount\":4000,\"orderedAt\":\"2026-07-10T14:00:00\"}";
		OrderCompletedEvent event = new OrderCompletedEvent(10L, 1L, 2L, 4000, LocalDateTime.of(2026, 7, 10, 14, 0));
		given(objectMapper.readValue(message, OrderCompletedEvent.class)).willReturn(event);

		// when
		consumer.consume(message);

		// then — 회원 1에게 주문번호·결제금액이 담긴 메시지 발송
		verify(notificationSender).send(eq(1L), contains("주문번호 10"));
		verify(notificationSender).send(eq(1L), contains("4000원"));
	}
}
