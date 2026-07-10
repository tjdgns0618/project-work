package com.example.projectwork.domain.order.entity;

import java.time.LocalDateTime;

import com.example.projectwork.domain.coffee.entity.Coffee;
import com.example.projectwork.domain.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "coffee_id", nullable = false)
	private Coffee coffee;

	@Column(nullable = false)
	private int payAmount;

	@Column(nullable = false)
	private LocalDateTime orderedAt;

	/**
	 * 주문 생성. 결제 금액은 주문 시점의 커피 가격을 스냅샷한다.
	 */
	public static Order create(Member member, Coffee coffee) {
		return new Order(null, member, coffee, coffee.getPrice(), LocalDateTime.now());
	}
}
