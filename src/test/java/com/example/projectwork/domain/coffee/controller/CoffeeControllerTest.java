package com.example.projectwork.domain.coffee.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.projectwork.domain.coffee.dto.CoffeeResponse;
import com.example.projectwork.domain.coffee.dto.PopularMenuResponse;
import com.example.projectwork.domain.coffee.service.CoffeeService;

@WebMvcTest(CoffeeController.class)
class CoffeeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CoffeeService coffeeService;

	@Test
	void 메뉴_목록_조회는_200과_메뉴_정보를_반환한다() throws Exception {
		// given
		given(coffeeService.getMenus()).willReturn(List.of(
				new CoffeeResponse(1L, "아메리카노", 4000)));

		// when & then
		mockMvc.perform(get("/api/v1/coffees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("커피 메뉴 조회가 완료되었습니다."))
				.andExpect(jsonPath("$.data[0].id").value(1))
				.andExpect(jsonPath("$.data[0].name").value("아메리카노"))
				.andExpect(jsonPath("$.data[0].price").value(4000));
	}

	@Test
	void 등록된_메뉴가_없으면_빈_배열을_반환한다() throws Exception {
		// given
		given(coffeeService.getMenus()).willReturn(List.of());

		// when & then
		mockMvc.perform(get("/api/v1/coffees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	void 인기_메뉴_조회는_200과_순위_목록을_반환한다() throws Exception {
		// given
		given(coffeeService.getPopularMenus()).willReturn(List.of(
				new PopularMenuResponse(1, 2L, "카페라떼", 4500, 120),
				new PopularMenuResponse(2, 1L, "아메리카노", 4000, 98)));

		// when & then
		mockMvc.perform(get("/api/v1/coffees/popular"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("인기 메뉴 조회가 완료되었습니다."))
				.andExpect(jsonPath("$.data[0].rank").value(1))
				.andExpect(jsonPath("$.data[0].id").value(2))
				.andExpect(jsonPath("$.data[0].orderCount").value(120));
	}
}
