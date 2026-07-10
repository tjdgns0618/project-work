package com.example.projectwork.domain.coffee.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.projectwork.domain.coffee.dto.CoffeeResponse;
import com.example.projectwork.domain.coffee.repository.CoffeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CoffeeService {

	private final CoffeeRepository coffeeRepository;

	@Transactional(readOnly = true)
	public List<CoffeeResponse> getMenus() {
		return coffeeRepository.findAll().stream()
				.map(CoffeeResponse::from)
				.toList();
	}
}
