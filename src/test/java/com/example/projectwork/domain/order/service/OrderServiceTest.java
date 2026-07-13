package com.example.projectwork.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private CoffeeRepository coffeeRepository;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private OrderService orderService;

	private Member member(long id) {
		Member member = Member.create("buyer@example.com", "hash", "김구매");
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

	private Coffee coffee(long id, int price) {
		Coffee coffee = Coffee.create("아메리카노", price);
		ReflectionTestUtils.setField(coffee, "id", id);
		return coffee;
	}

	@Test
	void 주문에_성공하면_포인트가_차감되고_이벤트가_발행되며_잔액을_반환한다() {
		// given
		given(memberRepository.findById(1L)).willReturn(Optional.of(member(1L)));
		given(coffeeRepository.findById(2L)).willReturn(Optional.of(coffee(2L, 4000)));
		given(memberRepository.deductPoint(1L, 4000L)).willReturn(1);
		given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
		given(memberRepository.findPointBalance(1L)).willReturn(6000L);

		// when
		OrderResponse response = orderService.order(new OrderCreateRequest(1L, 2L));

		// then
		assertThat(response.payAmount()).isEqualTo(4000);
		assertThat(response.pointBalance()).isEqualTo(6000L);

		ArgumentCaptor<OrderCompletedEvent> captor = ArgumentCaptor.forClass(OrderCompletedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().memberId()).isEqualTo(1L);
		assertThat(captor.getValue().coffeeId()).isEqualTo(2L);
		assertThat(captor.getValue().payAmount()).isEqualTo(4000);
	}

	@Test
	void 잔액이_부족하면_예외가_발생하고_저장과_이벤트가_없다() {
		// given — 조건부 UPDATE가 0행 (잔액 부족)
		given(memberRepository.findById(1L)).willReturn(Optional.of(member(1L)));
		given(coffeeRepository.findById(2L)).willReturn(Optional.of(coffee(2L, 4000)));
		given(memberRepository.deductPoint(1L, 4000L)).willReturn(0);

		// when & then
		assertThatThrownBy(() -> orderService.order(new OrderCreateRequest(1L, 2L)))
				.isInstanceOf(ServiceException.class)
				.hasMessage(MemberErrorCode.INSUFFICIENT_POINT.getMessage());
		verify(orderRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void 존재하지_않는_회원이면_예외가_발생한다() {
		// given
		given(memberRepository.findById(1L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> orderService.order(new OrderCreateRequest(1L, 2L)))
				.isInstanceOf(ServiceException.class)
				.hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
	}

	@Test
	void 존재하지_않는_메뉴이면_예외가_발생한다() {
		// given
		given(memberRepository.findById(1L)).willReturn(Optional.of(member(1L)));
		given(coffeeRepository.findById(2L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> orderService.order(new OrderCreateRequest(1L, 2L)))
				.isInstanceOf(ServiceException.class)
				.hasMessage(CoffeeErrorCode.COFFEE_NOT_FOUND.getMessage());
	}
}
