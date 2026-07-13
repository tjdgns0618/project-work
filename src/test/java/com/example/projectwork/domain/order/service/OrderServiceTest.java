package com.example.projectwork.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.projectwork.domain.coffee.entity.Coffee;
import com.example.projectwork.domain.member.entity.Member;
import com.example.projectwork.domain.order.entity.Order;
import com.example.projectwork.domain.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@InjectMocks
	private OrderService orderService;

	@Test
	void 주문을_생성해_저장하고_가격을_스냅샷한다() {
		// given
		Member member = Member.create("buyer@example.com", "hash", "김구매");
		Coffee coffee = Coffee.create("아메리카노", 4000);
		given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

		// when
		Order order = orderService.create(member, coffee);

		// then
		assertThat(order.getPayAmount()).isEqualTo(4000);
		assertThat(order.getMember()).isSameAs(member);
		assertThat(order.getCoffee()).isSameAs(coffee);
		verify(orderRepository).save(any(Order.class));
	}
}
