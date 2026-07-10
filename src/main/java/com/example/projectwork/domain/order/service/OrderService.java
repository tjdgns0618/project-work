package com.example.projectwork.domain.order.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.projectwork.domain.coffee.entity.Coffee;
import com.example.projectwork.domain.coffee.exception.CoffeeErrorCode;
import com.example.projectwork.domain.coffee.repository.CoffeeRepository;
import com.example.projectwork.domain.member.entity.Member;
import com.example.projectwork.domain.member.exception.MemberErrorCode;
import com.example.projectwork.domain.member.repository.MemberRepository;
import com.example.projectwork.domain.order.dto.OrderCreateRequest;
import com.example.projectwork.domain.order.dto.OrderResponse;
import com.example.projectwork.domain.order.entity.Order;
import com.example.projectwork.domain.order.event.OrderCompletedEvent;
import com.example.projectwork.domain.order.repository.OrderRepository;
import com.example.projectwork.global.exception.ServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final MemberRepository memberRepository;
	private final CoffeeRepository coffeeRepository;
	private final OrderRepository orderRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 주문/결제. 잔액 확인 → 포인트 차감 → 주문 저장을 한 트랜잭션으로 처리하고,
	 * 커밋 이후(리스너)에서 Kafka로 이벤트를 발행한다.
	 */
	@Transactional
	public OrderResponse order(OrderCreateRequest request) {
		Member member = memberRepository.findById(request.memberId())
				.orElseThrow(() -> new ServiceException(MemberErrorCode.MEMBER_NOT_FOUND));
		Coffee coffee = coffeeRepository.findById(request.coffeeId())
				.orElseThrow(() -> new ServiceException(CoffeeErrorCode.COFFEE_NOT_FOUND));

		member.usePoint(coffee.getPrice());
		Order order = orderRepository.save(Order.create(member, coffee));

		eventPublisher.publishEvent(new OrderCompletedEvent(
				member.getId(), coffee.getId(), order.getPayAmount(), order.getOrderedAt()));

		return OrderResponse.from(order);
	}
}
