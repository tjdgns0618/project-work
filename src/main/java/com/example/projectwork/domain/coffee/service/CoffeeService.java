package com.example.projectwork.domain.coffee.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.projectwork.domain.coffee.dto.CoffeeResponse;
import com.example.projectwork.domain.coffee.dto.PopularMenuResponse;
import com.example.projectwork.domain.coffee.entity.Coffee;
import com.example.projectwork.domain.coffee.exception.CoffeeErrorCode;
import com.example.projectwork.domain.coffee.ranking.PopularCount;
import com.example.projectwork.domain.coffee.ranking.PopularMenuRanking;
import com.example.projectwork.domain.coffee.repository.CoffeeRepository;
import com.example.projectwork.global.exception.ServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CoffeeService {

	private static final int POPULAR_DAYS = 7;
	private static final int POPULAR_LIMIT = 3;

	private final CoffeeRepository coffeeRepository;
	private final PopularMenuRanking popularMenuRanking;

	@Transactional(readOnly = true)
	public List<CoffeeResponse> getMenus() {
		return coffeeRepository.findAll().stream()
				.map(CoffeeResponse::from)
				.toList();
	}

	/** 메뉴 조회. 다른 도메인은 이 메서드를 통해서만 커피에 접근한다. */
	@Transactional(readOnly = true)
	public Coffee getCoffee(Long coffeeId) {
		return coffeeRepository.findById(coffeeId)
				.orElseThrow(() -> new ServiceException(CoffeeErrorCode.COFFEE_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public List<PopularMenuResponse> getPopularMenus() {
		List<PopularCount> top = popularMenuRanking.counts(LocalDate.now(), POPULAR_DAYS).stream()
				.sorted(Comparator.comparingLong(PopularCount::count).reversed()
						.thenComparingLong(PopularCount::coffeeId))
				.limit(POPULAR_LIMIT)
				.toList();

		List<Long> ids = top.stream().map(PopularCount::coffeeId).toList();
		Map<Long, Coffee> coffees = coffeeRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(Coffee::getId, Function.identity()));

		List<PopularMenuResponse> result = new ArrayList<>();
		int rank = 1;
		for (PopularCount pc : top) {
			Coffee coffee = coffees.get(pc.coffeeId());
			if (coffee == null) {
				continue;
			}
			result.add(new PopularMenuResponse(rank++, coffee.getId(), coffee.getName(),
					coffee.getPrice(), pc.count()));
		}
		return result;
	}
}
