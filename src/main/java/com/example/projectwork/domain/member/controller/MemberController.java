package com.example.projectwork.domain.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.projectwork.domain.member.dto.MemberCreateRequest;
import com.example.projectwork.domain.member.dto.MemberResponse;
import com.example.projectwork.domain.member.dto.PointChargeRequest;
import com.example.projectwork.domain.member.dto.PointChargeResponse;
import com.example.projectwork.domain.member.service.MemberService;
import com.example.projectwork.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

	private final MemberService memberService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<MemberResponse> signUp(@Valid @RequestBody MemberCreateRequest request) {
		return ApiResponse.success("회원 가입이 완료되었습니다.", memberService.signUp(request));
	}

	@PostMapping("/{memberId}/points")
	public ApiResponse<PointChargeResponse> chargePoint(
			@PathVariable Long memberId,
			@Valid @RequestBody PointChargeRequest request) {
		return ApiResponse.success("포인트 충전이 완료되었습니다.",
				memberService.chargePoint(memberId, request.amount()));
	}
}
