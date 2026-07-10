package com.example.projectwork.domain.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.projectwork.domain.order.dto.OrderCreateRequest;
import com.example.projectwork.domain.order.dto.OrderResponse;
import com.example.projectwork.domain.order.service.OrderService;
import com.example.projectwork.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<OrderResponse> order(@Valid @RequestBody OrderCreateRequest request) {
		return ApiResponse.success("주문 및 결제가 완료되었습니다.", orderService.order(request));
	}
}
