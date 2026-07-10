package com.example.projectwork.domain.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.projectwork.domain.member.dto.MemberResponse;
import com.example.projectwork.domain.member.dto.PointChargeResponse;
import com.example.projectwork.domain.member.exception.MemberErrorCode;
import com.example.projectwork.domain.member.service.MemberService;
import com.example.projectwork.global.exception.ServiceException;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberService memberService;

	@Test
	void 회원가입_요청이_성공하면_201과_회원정보를_반환한다() throws Exception {
		// given
		given(memberService.signUp(any()))
				.willReturn(new MemberResponse(1L, "buyer@example.com", "김구매"));
		String body = """
				{"email":"buyer@example.com","password":"P@ssw0rd!","name":"김구매"}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/members")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("회원 가입이 완료되었습니다."))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.email").value("buyer@example.com"));
	}

	@Test
	void 이메일이_중복되면_409를_반환한다() throws Exception {
		// given
		given(memberService.signUp(any()))
				.willThrow(new ServiceException(MemberErrorCode.EMAIL_DUPLICATED));
		String body = """
				{"email":"buyer@example.com","password":"P@ssw0rd!","name":"김구매"}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/members")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(MemberErrorCode.EMAIL_DUPLICATED.getMessage()));
	}

	@Test
	void 필수값이_누락되면_400을_반환한다() throws Exception {
		// given — email 공백
		String body = """
				{"email":"","password":"P@ssw0rd!","name":"김구매"}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/members")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 본문_JSON이_깨지면_400을_반환한다() throws Exception {
		// given — 파싱 불가능한 JSON
		String malformed = "{ \"email\": ";

		// when & then
		mockMvc.perform(post("/api/v1/members")
						.contentType(MediaType.APPLICATION_JSON)
						.content(malformed))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 데이터_제약_위반이면_409를_반환한다() throws Exception {
		// given
		given(memberService.signUp(any()))
				.willThrow(new DataIntegrityViolationException("duplicate"));
		String body = """
				{"email":"buyer@example.com","password":"P@ssw0rd!","name":"김구매"}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/members")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void 포인트_충전_요청이_성공하면_200과_잔액을_반환한다() throws Exception {
		// given
		given(memberService.chargePoint(eq(1L), anyLong()))
				.willReturn(new PointChargeResponse(1L, 10000L));
		String body = """
				{"amount":10000}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/members/1/points")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.memberId").value(1))
				.andExpect(jsonPath("$.data.pointBalance").value(10000));
	}

	@Test
	void 충전액이_0이하이면_400을_반환한다() throws Exception {
		// given
		String body = """
				{"amount":0}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/members/1/points")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 존재하지_않는_회원_충전은_404를_반환한다() throws Exception {
		// given
		given(memberService.chargePoint(eq(1L), anyLong()))
				.willThrow(new ServiceException(MemberErrorCode.MEMBER_NOT_FOUND));
		String body = """
				{"amount":10000}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/members/1/points")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound());
	}
}
