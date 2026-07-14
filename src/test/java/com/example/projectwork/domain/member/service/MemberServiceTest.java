package com.example.projectwork.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.projectwork.domain.member.dto.MemberCreateRequest;
import com.example.projectwork.domain.member.dto.MemberResponse;
import com.example.projectwork.domain.member.dto.PointChargeResponse;
import com.example.projectwork.domain.member.entity.Member;
import com.example.projectwork.domain.member.exception.MemberErrorCode;
import com.example.projectwork.domain.member.repository.MemberRepository;
import com.example.projectwork.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private MemberService memberService;

	@Test
	void 회원가입에_성공하면_회원_정보를_반환한다() {
		// given
		MemberCreateRequest request = new MemberCreateRequest("buyer@example.com", "P@ssw0rd!", "김구매");
		given(memberRepository.existsByEmail(request.email())).willReturn(false);
		given(passwordEncoder.encode(request.password())).willReturn("hashed");
		given(memberRepository.save(any(Member.class)))
				.willReturn(Member.create(request.email(), "hashed", request.name()));

		// when
		MemberResponse response = memberService.signUp(request);

		// then
		assertThat(response.email()).isEqualTo("buyer@example.com");
		assertThat(response.name()).isEqualTo("김구매");
		verify(passwordEncoder).encode("P@ssw0rd!");
	}

	@Test
	void 이메일이_중복되면_예외가_발생한다() {
		// given
		MemberCreateRequest request = new MemberCreateRequest("buyer@example.com", "P@ssw0rd!", "김구매");
		given(memberRepository.existsByEmail(request.email())).willReturn(true);

		// when & then
		assertThatThrownBy(() -> memberService.signUp(request))
				.isInstanceOf(ServiceException.class)
				.hasMessage(MemberErrorCode.EMAIL_DUPLICATED.getMessage());
		verify(memberRepository, never()).save(any());
	}

	@Test
	void 포인트_충전에_성공하면_갱신된_잔액을_반환한다() {
		// given
		given(memberRepository.chargePoint(1L, 10000L)).willReturn(1);
		given(memberRepository.findPointBalance(1L)).willReturn(10000L);

		// when
		PointChargeResponse response = memberService.chargePoint(1L, 10000L);

		// then
		assertThat(response.memberId()).isEqualTo(1L);
		assertThat(response.pointBalance()).isEqualTo(10000L);
	}

	@Test
	void 존재하지_않는_회원_충전은_예외가_발생한다() {
		// given
		given(memberRepository.chargePoint(1L, 10000L)).willReturn(0);

		// when & then
		assertThatThrownBy(() -> memberService.chargePoint(1L, 10000L))
				.isInstanceOf(ServiceException.class)
				.hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
	}

	@Test
	void 회원을_조회한다() {
		// given
		Member member = Member.create("buyer@example.com", "hash", "김구매");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		// when & then
		assertThat(memberService.getMember(1L)).isSameAs(member);
	}

	@Test
	void 존재하지_않는_회원_조회는_예외가_발생한다() {
		// given
		given(memberRepository.findById(1L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> memberService.getMember(1L))
				.isInstanceOf(ServiceException.class)
				.hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
	}

	@Test
	void 포인트_사용에_성공하면_갱신된_잔액을_반환한다() {
		// given
		given(memberRepository.deductPoint(1L, 4000L)).willReturn(1);
		given(memberRepository.findPointBalance(1L)).willReturn(6000L);

		// when & then
		assertThat(memberService.usePoint(1L, 4000L)).isEqualTo(6000L);
	}

	@Test
	void 잔액이_부족하면_예외가_발생한다() {
		// given
		given(memberRepository.deductPoint(1L, 4000L)).willReturn(0);

		// when & then
		assertThatThrownBy(() -> memberService.usePoint(1L, 4000L))
				.isInstanceOf(ServiceException.class)
				.hasMessage(MemberErrorCode.INSUFFICIENT_POINT.getMessage());
	}
}
