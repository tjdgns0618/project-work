package com.example.projectwork.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.projectwork.domain.member.dto.MemberCreateRequest;
import com.example.projectwork.domain.member.dto.MemberResponse;
import com.example.projectwork.domain.member.dto.PointChargeResponse;
import com.example.projectwork.domain.member.entity.Member;
import com.example.projectwork.domain.member.exception.MemberErrorCode;
import com.example.projectwork.domain.member.repository.MemberRepository;
import com.example.projectwork.global.exception.ServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public MemberResponse signUp(MemberCreateRequest request) {
		if (memberRepository.existsByEmail(request.email())) {
			throw new ServiceException(MemberErrorCode.EMAIL_DUPLICATED);
		}
		String passwordHash = passwordEncoder.encode(request.password());
		Member member = memberRepository.save(
				Member.create(request.email(), passwordHash, request.name()));
		return MemberResponse.from(member);
	}

	/** 원자적 UPDATE로 충전한다. 동시 충전에도 합계가 정확하다. */
	@Transactional
	public PointChargeResponse chargePoint(Long memberId, long amount) {
		int updated = memberRepository.chargePoint(memberId, amount);
		if (updated == 0) {
			throw new ServiceException(MemberErrorCode.MEMBER_NOT_FOUND);
		}
		return new PointChargeResponse(memberId, memberRepository.findPointBalance(memberId));
	}

	/** 회원 조회. 다른 도메인은 이 메서드를 통해서만 회원에 접근한다. */
	@Transactional(readOnly = true)
	public Member getMember(Long memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new ServiceException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	/**
	 * 포인트를 원자적으로 차감하고 갱신된 잔액을 반환한다. 잔액이 부족하면 예외.
	 * (회원 존재는 호출 측에서 {@link #getMember}로 보장한다.)
	 */
	@Transactional
	public long usePoint(Long memberId, long amount) {
		int deducted = memberRepository.deductPoint(memberId, amount);
		if (deducted == 0) {
			throw new ServiceException(MemberErrorCode.INSUFFICIENT_POINT);
		}
		return memberRepository.findPointBalance(memberId);
	}
}
