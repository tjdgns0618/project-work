package com.example.projectwork.domain.member.dto;

import com.example.projectwork.domain.member.entity.Member;

public record PointChargeResponse(Long memberId, Long pointBalance) {

	public static PointChargeResponse from(Member member) {
		return new PointChargeResponse(member.getId(), member.getPointBalance());
	}
}
