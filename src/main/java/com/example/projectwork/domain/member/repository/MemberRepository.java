package com.example.projectwork.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.projectwork.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmail(String email);

	@Query("SELECT m.pointBalance FROM Member m WHERE m.id = :id")
	Long findPointBalance(@Param("id") Long id);

	/** 원자적 충전. 영향 행 0이면 회원 부재. */
	@Transactional
	@Modifying
	@Query("UPDATE Member m SET m.pointBalance = m.pointBalance + :amount WHERE m.id = :id")
	int chargePoint(@Param("id") Long id, @Param("amount") long amount);

	/** 원자적 차감. 잔액이 충분할 때만 차감하며, 영향 행 0이면 회원 부재 또는 잔액 부족. */
	@Transactional
	@Modifying
	@Query("UPDATE Member m SET m.pointBalance = m.pointBalance - :amount"
			+ " WHERE m.id = :id AND m.pointBalance >= :amount")
	int deductPoint(@Param("id") Long id, @Param("amount") long amount);
}
