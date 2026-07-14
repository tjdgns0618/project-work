package com.example.projectwork.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.projectwork.domain.coffee.entity.Coffee;
import com.example.projectwork.domain.coffee.service.CoffeeService;
import com.example.projectwork.domain.member.entity.Member;
import com.example.projectwork.domain.member.exception.MemberErrorCode;
import com.example.projectwork.domain.member.service.MemberService;
import com.example.projectwork.domain.order.dto.OrderCreateRequest;
import com.example.projectwork.domain.order.dto.OrderResponse;
import com.example.projectwork.domain.order.entity.Order;
import com.example.projectwork.domain.order.event.OrderCompletedEvent;
import com.example.projectwork.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

	@Mock
	private MemberService memberService;

	@Mock
	private CoffeeService coffeeService;

	@Mock
	private OrderService orderService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private OrderFacade orderFacade;

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
	void 도메인_서비스를_조합해_주문을_처리하고_이벤트를_발행한다() {
		// given
		Member member = member(1L);
		Coffee coffee = coffee(2L, 4000);
		Order order = Order.create(member, coffee);
		ReflectionTestUtils.setField(order, "id", 10L);
		given(memberService.getMember(1L)).willReturn(member);
		given(coffeeService.getCoffee(2L)).willReturn(coffee);
		given(memberService.usePoint(1L, 4000L)).willReturn(6000L);
		given(orderService.create(member, coffee)).willReturn(order);

		// when
		OrderResponse response = orderFacade.placeOrder(new OrderCreateRequest(1L, 2L));

		// then
		assertThat(response.orderId()).isEqualTo(10L);
		assertThat(response.payAmount()).isEqualTo(4000);
		assertThat(response.pointBalance()).isEqualTo(6000L);

		ArgumentCaptor<OrderCompletedEvent> captor = ArgumentCaptor.forClass(OrderCompletedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().orderId()).isEqualTo(10L);
		assertThat(captor.getValue().memberId()).isEqualTo(1L);
		assertThat(captor.getValue().coffeeId()).isEqualTo(2L);
		assertThat(captor.getValue().payAmount()).isEqualTo(4000);
	}

	@Test
	void 잔액이_부족하면_예외가_전파되고_주문_저장과_이벤트가_없다() {
		// given
		Member member = member(1L);
		Coffee coffee = coffee(2L, 4000);
		given(memberService.getMember(1L)).willReturn(member);
		given(coffeeService.getCoffee(2L)).willReturn(coffee);
		given(memberService.usePoint(1L, 4000L))
				.willThrow(new ServiceException(MemberErrorCode.INSUFFICIENT_POINT));

		// when & then
		assertThatThrownBy(() -> orderFacade.placeOrder(new OrderCreateRequest(1L, 2L)))
				.isInstanceOf(ServiceException.class)
				.hasMessage(MemberErrorCode.INSUFFICIENT_POINT.getMessage());
		verify(orderService, never()).create(any(), any());
		verify(eventPublisher, never()).publishEvent(any());
	}
}
