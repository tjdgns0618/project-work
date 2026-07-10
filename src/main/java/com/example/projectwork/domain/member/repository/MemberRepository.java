package com.example.projectwork.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.projectwork.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmail(String email);
}
