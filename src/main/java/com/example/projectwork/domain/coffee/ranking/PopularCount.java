package com.example.projectwork.domain.coffee.ranking;

/** 특정 메뉴의 집계 주문 횟수. */
public record PopularCount(Long coffeeId, long count) {
}
