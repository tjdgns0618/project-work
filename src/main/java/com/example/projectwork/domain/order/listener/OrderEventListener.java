package com.example.projectwork.domain.order.listener;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.projectwork.domain.order.event.OrderCompletedEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

	private static final String TOPIC = "order.completed";

	private final KafkaTemplate<Object, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * 결제 커밋 이후 주문 이벤트를 JSON으로 직렬화해 Kafka로 발행한다.
	 * 이미 커밋된 뒤이므로 발행 실패가 결제를 롤백하지 않는다.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publish(OrderCompletedEvent event) {
		kafkaTemplate.send(TOPIC, objectMapper.writeValueAsString(event));
	}
}
