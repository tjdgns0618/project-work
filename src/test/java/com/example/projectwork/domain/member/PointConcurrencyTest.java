package com.example.projectwork.domain.member;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.projectwork.domain.member.entity.Member;
import com.example.projectwork.domain.member.repository.MemberRepository;
import com.example.projectwork.domain.order.repository.OrderRepository;

/**
 * 포인트 잔액의 원자적 UPDATE가 동시성에서 정확한지 실제 DB로 검증한다.
 * (read-modify-write였다면 lost update / 이중 차감으로 실패할 시나리오들)
 */
@SpringBootTest
class PointConcurrencyTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private OrderRepository orderRepository;

	@AfterEach
	void tearDown() {
		orderRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	void 동시에_충전해도_잔액이_정확하다() throws InterruptedException {
		// given
		Member member = memberRepository.save(Member.create("charge@test.com", "hash", "동시충전"));
		long memberId = member.getId();
		int threads = 50;
		long amount = 1000L;

		// when — 50개 스레드가 동시에 1000씩 충전
		runConcurrently(threads, () -> memberRepository.chargePoint(memberId, amount));

		// then — lost update 없이 합계가 정확
		assertThat(memberRepository.findPointBalance(memberId)).isEqualTo(threads * amount);
	}

	@Test
	void 동시에_주문해도_초과_차감되지_않고_잔액이_음수가_되지_않는다() throws InterruptedException {
		// given — 딱 10건치 잔액
		Member member = memberRepository.save(Member.create("deduct@test.com", "hash", "동시주문"));
		long memberId = member.getId();
		int price = 4000;
		int affordable = 10;
		memberRepository.chargePoint(memberId, (long) price * affordable);
		int threads = 50;
		AtomicInteger success = new AtomicInteger();

		// when — 50개 스레드가 동시에 4000씩 차감 시도
		runConcurrently(threads, () -> {
			if (memberRepository.deductPoint(memberId, price) == 1) {
				success.incrementAndGet();
			}
		});

		// then — 정확히 10건만 성공, 나머지는 잔액 부족, 최종 잔액 0(음수 없음)
		assertThat(success.get()).isEqualTo(affordable);
		assertThat(memberRepository.findPointBalance(memberId)).isEqualTo(0L);
	}

	private void runConcurrently(int threads, Runnable task) throws InterruptedException {
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		for (int i = 0; i < threads; i++) {
			pool.submit(() -> {
				ready.countDown();
				try {
					start.await();
					task.run();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}
		ready.await();
		start.countDown();
		done.await(30, TimeUnit.SECONDS);
		pool.shutdown();
	}
}
