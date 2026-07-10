package com.example.projectwork.domain.coffee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.projectwork.domain.coffee.dto.CoffeeResponse;
import com.example.projectwork.domain.coffee.entity.Coffee;
import com.example.projectwork.domain.coffee.repository.CoffeeRepository;

@ExtendWith(MockitoExtension.class)
class CoffeeServiceTest {

	@Mock
	private CoffeeRepository coffeeRepository;

	@InjectMocks
	private CoffeeService coffeeService;

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
}
