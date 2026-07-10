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

	@Transactional
	public PointChargeResponse chargePoint(Long memberId, long amount) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new ServiceException(MemberErrorCode.MEMBER_NOT_FOUND));
		member.chargePoint(amount);
		return PointChargeResponse.from(member);
	}
}
