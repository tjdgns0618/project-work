package com.example.projectwork.domain.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.projectwork.domain.coffee.entity.Coffee;
import com.example.projectwork.domain.member.entity.Member;
import com.example.projectwork.domain.order.entity.Order;
import com.example.projectwork.domain.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * 순수 order 도메인 서비스 — 주문 생성/저장만 담당한다.
 * 회원·메뉴 조회나 포인트 차감 같은 타 도메인 흐름은 {@link OrderFacade}가 조합한다.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;

	@Transactional
	public Order create(Member member, Coffee coffee) {
		return orderRepository.save(Order.create(member, coffee));
	}
}
