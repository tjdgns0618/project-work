package com.example.projectwork.domain.coffee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.projectwork.domain.coffee.dto.CoffeeResponse;
import com.example.projectwork.domain.coffee.dto.PopularMenuResponse;
import com.example.projectwork.domain.coffee.entity.Coffee;
import com.example.projectwork.domain.coffee.ranking.PopularCount;
import com.example.projectwork.domain.coffee.ranking.PopularMenuRanking;
import com.example.projectwork.domain.coffee.repository.CoffeeRepository;

@ExtendWith(MockitoExtension.class)
class CoffeeServiceTest {

	@Mock
	private CoffeeRepository coffeeRepository;

	@Mock
	private PopularMenuRanking popularMenuRanking;

	@InjectMocks
	private CoffeeService coffeeService;

	private Coffee coffee(long id, String name, int price) {
		Coffee coffee = Coffee.create(name, price);
		ReflectionTestUtils.setField(coffee, "id", id);
		return coffee;
	}

	@Test
	void 등록된_메뉴_목록을_반환한다() {
		// given
		given(coffeeRepository.findAll()).willReturn(List.of(
				Coffee.create("아메리카노", 4000),
				Coffee.create("카페라떼", 4500)));

		// when
		List<CoffeeResponse> result = coffeeService.getMenus();

		// then
		assertThat(result).hasSize(2);
		assertThat(result).extracting(CoffeeResponse::name)
				.containsExactly("아메리카노", "카페라떼");
	}

	@Test
	void 등록된_메뉴가_없으면_빈_목록을_반환한다() {
		// given
		given(coffeeRepository.findAll()).willReturn(List.of());

		// when & then
		assertThat(coffeeService.getMenus()).isEmpty();
	}

	@Test
	void 인기_메뉴는_주문수_내림차순_동점시_ID_오름차순으로_최대_3개를_반환한다() {
		// given — Redis 집계 결과(정렬 전). id 1과 5가 98로 동점, 3은 하위
		given(popularMenuRanking.counts(any(), eq(7))).willReturn(List.of(
				new PopularCount(1L, 98),
				new PopularCount(2L, 120),
				new PopularCount(5L, 98),
				new PopularCount(3L, 50)));
		given(coffeeRepository.findAllById(any())).willReturn(List.of(
				coffee(2L, "카페라떼", 4500),
				coffee(1L, "아메리카노", 4000),
				coffee(5L, "바닐라라떼", 5000)));

		// when
		List<PopularMenuResponse> result = coffeeService.getPopularMenus();

		// then — 120 → 98(id 1) → 98(id 5), 3위까지
		assertThat(result)
				.extracting(PopularMenuResponse::rank, PopularMenuResponse::id, PopularMenuResponse::orderCount)
				.containsExactly(
						tuple(1, 2L, 120L),
						tuple(2, 1L, 98L),
						tuple(3, 5L, 98L));
	}

	@Test
	void 인기_메뉴_집계가_없으면_빈_목록을_반환한다() {
		// given
		given(popularMenuRanking.counts(any(), eq(7))).willReturn(List.of());

		// when & then
		assertThat(coffeeService.getPopularMenus()).isEmpty();
	}
}
