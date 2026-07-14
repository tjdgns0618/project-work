package com.example.projectwork.domain.order.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.projectwork.domain.coffee.entity.Coffee;
import com.example.projectwork.domain.coffee.service.CoffeeService;
import com.example.projectwork.domain.member.entity.Member;
import com.example.projectwork.domain.member.service.MemberService;
import com.example.projectwork.domain.order.dto.OrderCreateRequest;
import com.example.projectwork.domain.order.dto.OrderResponse;
import com.example.projectwork.domain.order.entity.Order;
import com.example.projectwork.domain.order.event.OrderCompletedEvent;

import lombok.RequiredArgsConstructor;

/**
 * 주문/결제 유스케이스 조합(application service). 각 도메인은 자기 Service로만 접근하고,
 * 타 도메인 Repository를 직접 참조하지 않는다.
 * 잔액 확인·차감(원자)·주문 저장을 한 트랜잭션으로 묶고, 커밋 이후 이벤트가 Kafka로 발행된다.
 */
@Service
@RequiredArgsConstructor
public class OrderFacade {

	private final MemberService memberService;
	private final CoffeeService coffeeService;
	private final OrderService orderService;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public OrderResponse placeOrder(OrderCreateRequest request) {
		Member member = memberService.getMember(request.memberId());
		Coffee coffee = coffeeService.getCoffee(request.coffeeId());

		long pointBalance = memberService.usePoint(member.getId(), coffee.getPrice());
		Order order = orderService.create(member, coffee);

		eventPublisher.publishEvent(new OrderCompletedEvent(
				member.getId(), coffee.getId(), order.getPayAmount(), order.getOrderedAt()));

		return OrderResponse.from(order, pointBalance);
	}
}
