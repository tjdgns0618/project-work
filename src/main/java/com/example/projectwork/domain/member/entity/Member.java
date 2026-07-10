package com.example.projectwork.domain.member.entity;

import com.example.projectwork.domain.member.exception.MemberErrorCode;
import com.example.projectwork.global.exception.ServiceException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private Long pointBalance;

	public static Member create(String email, String passwordHash, String name) {
		return new Member(null, email, passwordHash, name, 0L);
	}

	public void chargePoint(long amount) {
		this.pointBalance += amount;
	}

	public void usePoint(long amount) {
		if (this.pointBalance < amount) {
			throw new ServiceException(MemberErrorCode.INSUFFICIENT_POINT);
		}
		this.pointBalance -= amount;
	}
}
