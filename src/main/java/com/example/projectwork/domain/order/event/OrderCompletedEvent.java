package com.example.projectwork.domain.order.event;

import java.time.LocalDateTime;

/**
 * 결제 완료 주문 이벤트. 데이터 수집 플랫폼 전송·인기 메뉴 적재의 공통 소스.
 * 페이로드: 사용자 식별값 · 메뉴 ID · 결제 금액 · 주문 시각.
 */
public record OrderCompletedEvent(Long memberId, Long coffeeId, int payAmount, LocalDateTime orderedAt) {
}
