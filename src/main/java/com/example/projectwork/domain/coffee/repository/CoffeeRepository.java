package com.example.projectwork.domain.coffee.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.projectwork.domain.coffee.entity.Coffee;

public interface CoffeeRepository extends JpaRepository<Coffee, Long> {
}
