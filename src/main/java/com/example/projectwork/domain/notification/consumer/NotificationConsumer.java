package com.example.projectwork.domain.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.projectwork.domain.notification.sender.NotificationSender;
import com.example.projectwork.domain.order.event.OrderCompletedEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * 주문 완료 알림 소비자. 인기 메뉴 소비자와 별개인 **독립 consumer group**(notification-service)이라,
 * 발행자·기존 소비자를 수정하지 않고 order.completed에 추가로 붙는다 — Kafka fan-out 확장의 예.
 */
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

	private final NotificationSender notificationSender;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "order.completed", groupId = "notification-service")
	public void consume(String message) {
		OrderCompletedEvent event = objectMapper.readValue(message, OrderCompletedEvent.class);
		String text = "주문이 완료되었습니다. (주문번호 " + event.orderId()
				+ ", 결제금액 " + event.payAmount() + "원)";
		notificationSender.send(event.memberId(), text);
	}
}
