package com.example.projectwork.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.projectwork.domain.coffee.entity.Coffee;
import com.example.projectwork.domain.coffee.repository.CoffeeRepository;
import com.example.projectwork.domain.member.entity.Member;
import com.example.projectwork.domain.member.repository.MemberRepository;
import com.example.projectwork.domain.order.dto.OrderCreateRequest;
import com.example.projectwork.domain.order.repository.OrderRepository;

/**
 * 주문 유스케이스의 실제 트랜잭션 경계를 검증한다.
 * (OrderFacadeTest는 Mockito 단위라 Spring 프록시·@Transactional이 작동하지 않으므로 별도 통합 테스트로 증명)
 * - 정상 주문: 포인트 차감 + 주문 저장 + 커밋 이후 이벤트 발행
 * - 주문 저장 실패: 포인트 차감까지 롤백, 이벤트 미발행
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderFacadeIntegrationTest {

	@Autowired
	private OrderFacade orderFacade;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private CoffeeRepository coffeeRepository;
	@Autowired
	private OrderRepository orderRepository;

	@MockitoBean
	private KafkaTemplate<Object, Object> kafkaTemplate; // 커밋 후 발행 여부만 관찰
	@MockitoSpyBean
	private OrderService orderService;                   // 기본은 실제 동작, 실패 주입 시에만 스텁

	private Long memberId;
	private Long coffeeId;

	@BeforeEach
	void setUp() {
		memberId = memberRepository.save(Member.create("buyer@example.com", "hash", "김구매")).getId();
		memberRepository.chargePoint(memberId, 10_000L);
		coffeeId = coffeeRepository.save(Coffee.create("아메리카노", 4000)).getId();
	}

	@AfterEach
	void tearDown() {
		orderRepository.deleteAll();
		memberRepository.deleteAll();
		coffeeRepository.deleteAll();
	}

	@Test
	void 정상_주문이면_포인트가_차감되고_주문이_저장되며_커밋_후_이벤트가_발행된다() {
		// given
		CompletableFuture<SendResult<Object, Object>> ok = CompletableFuture.completedFuture(null);
		given(kafkaTemplate.send(anyString(), any())).willReturn(ok);

		// when
		orderFacade.placeOrder(new OrderCreateRequest(memberId, coffeeId));

		// then — 잔액 차감(10000-4000) + 주문 저장
		assertThat(memberRepository.findPointBalance(memberId)).isEqualTo(6_000L);
		assertThat(orderRepository.count()).isEqualTo(1);
		// 커밋 이후 AFTER_COMMIT 리스너가 실행되어 발행됨
		verify(kafkaTemplate).send(anyString(), any());
	}

	@Test
	void 주문_저장이_실패하면_포인트_차감도_롤백되고_이벤트가_발행되지_않는다() {
		// given — 주문 저장 단계에서 예외 주입(포인트 차감 이후 실패)
		doThrow(new RuntimeException("주문 저장 실패")).when(orderService).create(any(), any());

		// when & then
		assertThatThrownBy(() -> orderFacade.placeOrder(new OrderCreateRequest(memberId, coffeeId)))
				.isInstanceOf(RuntimeException.class);

		// 포인트 차감 롤백(잔액 유지) · 주문 없음 · 커밋 안 됐으므로 이벤트 미발행
		assertThat(memberRepository.findPointBalance(memberId)).isEqualTo(10_000L);
		assertThat(orderRepository.count()).isEqualTo(0);
		verify(kafkaTemplate, never()).send(anyString(), any());
	}
}
