package com.example.projectwork.domain.coffee.ranking;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 인기 메뉴 랭킹의 Redis Sorted Set 접근 계층.
 * 일자별 키(popular:coffee:yyyyMMdd)에 주문 횟수를 누적하고, 최근 N일을 합산해 조회한다.
 */
@Component
@RequiredArgsConstructor
public class PopularMenuRanking {

	private static final String KEY_PREFIX = "popular:coffee:";
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final Duration DAILY_TTL = Duration.ofDays(8);
	private static final Duration UNION_TTL = Duration.ofMinutes(1);

	private final StringRedisTemplate redisTemplate;

	/** 주문 1건을 해당 일자 랭킹에 반영한다. */
	public void increment(Long coffeeId, LocalDate date) {
		String key = dayKey(date);
		redisTemplate.opsForZSet().incrementScore(key, String.valueOf(coffeeId), 1);
		redisTemplate.expire(key, DAILY_TTL);
	}

	/**
	 * 오늘 포함 최근 days일을 합산한 (coffeeId, count) 목록. 정렬·상위 N 추출은 호출측 책임이다.
	 * (Redis는 동점 시 멤버 사전순이라 순위 정렬은 애플리케이션에서 수행한다.)
	 */
	public List<PopularCount> counts(LocalDate today, int days) {
		List<String> keys = new ArrayList<>();
		for (int i = 0; i < days; i++) {
			keys.add(dayKey(today.minusDays(i)));
		}
		String destKey = KEY_PREFIX + "union:" + today.format(DAY);
		redisTemplate.opsForZSet().unionAndStore(keys.get(0), keys.subList(1, keys.size()), destKey);
		redisTemplate.expire(destKey, UNION_TTL);

		Set<ZSetOperations.TypedTuple<String>> tuples =
				redisTemplate.opsForZSet().reverseRangeWithScores(destKey, 0, -1);
		if (tuples == null || tuples.isEmpty()) {
			return List.of();
		}
		List<PopularCount> result = new ArrayList<>();
		for (ZSetOperations.TypedTuple<String> tuple : tuples) {
			if (tuple.getValue() != null && tuple.getScore() != null) {
				result.add(new PopularCount(Long.parseLong(tuple.getValue()), tuple.getScore().longValue()));
			}
		}
		return result;
	}

	private String dayKey(LocalDate date) {
		return KEY_PREFIX + date.format(DAY);
	}
}
