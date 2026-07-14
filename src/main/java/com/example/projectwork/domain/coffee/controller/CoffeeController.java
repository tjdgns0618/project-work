package com.example.projectwork.domain.coffee.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.projectwork.domain.coffee.dto.CoffeeResponse;
import com.example.projectwork.domain.coffee.dto.PopularMenuResponse;
import com.example.projectwork.domain.coffee.service.CoffeeService;
import com.example.projectwork.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coffees")
public class CoffeeController {

	private final CoffeeService coffeeService;

	@GetMapping
	public ApiResponse<List<CoffeeResponse>> getMenus() {
		return ApiResponse.success("커피 메뉴 조회가 완료되었습니다.", coffeeService.getMenus());
	}

	@GetMapping("/popular")
	public ApiResponse<List<PopularMenuResponse>> getPopularMenus() {
		return ApiResponse.success("인기 메뉴 조회가 완료되었습니다.", coffeeService.getPopularMenus());
	}
}
