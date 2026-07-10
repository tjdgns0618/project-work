package com.example.projectwork.domain.coffee.dto;

public record PopularMenuResponse(int rank, Long id, String name, int price, long orderCount) {
}
