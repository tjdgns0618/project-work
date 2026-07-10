package com.example.projectwork.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.example.projectwork.domain.member.exception.MemberErrorCode;
import com.example.projectwork.global.exception.ServiceException;

class MemberTest {

	@Test
	void 포인트를_충전하면_잔액이_증가한다() {
		// given
		Member member = Member.create("buyer@example.com", "hash", "김구매");

		// when
		member.chargePoint(10000L);

		// then
		assertThat(member.getPointBalance()).isEqualTo(10000L);
	}

	@Test
	void 잔액이_충분하면_포인트가_차감된다() {
		// given
		Member member = Member.create("buyer@example.com", "hash", "김구매");
		member.chargePoint(10000L);

		// when
		member.usePoint(4500L);

		// then
		assertThat(member.getPointBalance()).isEqualTo(5500L);
	}

	@Test
	void 잔액이_부족하면_예외가_발생한다() {
		// given
		Member member = Member.create("buyer@example.com", "hash", "김구매");
		member.chargePoint(1000L);

		// when & then
		assertThatThrownBy(() -> member.usePoint(2000L))
				.isInstanceOf(ServiceException.class)
				.hasMessage(MemberErrorCode.INSUFFICIENT_POINT.getMessage());
	}
}
