package com.example.projectwork.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.projectwork.domain.member.exception.MemberErrorCode;
import com.example.projectwork.domain.order.dto.OrderResponse;
import com.example.projectwork.domain.order.service.OrderFacade;
import com.example.projectwork.global.exception.ServiceException;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderFacade orderFacade;

	@Test
	void 주문_요청이_성공하면_201과_주문정보를_반환한다() throws Exception {
		// given
		given(orderFacade.placeOrder(any())).willReturn(
				new OrderResponse(1L, 1L, 2L, 4500, 5500L, LocalDateTime.parse("2026-07-10T14:00:00")));
		String body = """
				{"memberId":1,"coffeeId":2}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("주문 및 결제가 완료되었습니다."))
				.andExpect(jsonPath("$.data.orderId").value(1))
				.andExpect(jsonPath("$.data.payAmount").value(4500))
				.andExpect(jsonPath("$.data.pointBalance").value(5500));
	}

	@Test
	void 필수값이_누락되면_400을_반환한다() throws Exception {
		// given — coffeeId 누락
		String body = """
				{"memberId":1}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 잔액이_부족하면_409를_반환한다() throws Exception {
		// given
		given(orderFacade.placeOrder(any()))
				.willThrow(new ServiceException(MemberErrorCode.INSUFFICIENT_POINT));
		String body = """
				{"memberId":1,"coffeeId":2}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void 존재하지_않는_회원이면_404를_반환한다() throws Exception {
		// given
		given(orderFacade.placeOrder(any()))
				.willThrow(new ServiceException(MemberErrorCode.MEMBER_NOT_FOUND));
		String body = """
				{"memberId":1,"coffeeId":2}
				""";

		// when & then
		mockMvc.perform(post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound());
	}
}
