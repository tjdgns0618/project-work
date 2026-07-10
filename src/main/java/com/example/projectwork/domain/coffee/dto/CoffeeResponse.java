package com.example.projectwork.domain.coffee.dto;

import com.example.projectwork.domain.coffee.entity.Coffee;

public record CoffeeResponse(Long id, String name, int price) {

	public static CoffeeResponse from(Coffee coffee) {
		return new CoffeeResponse(coffee.getId(), coffee.getName(), coffee.getPrice());
	}
}
