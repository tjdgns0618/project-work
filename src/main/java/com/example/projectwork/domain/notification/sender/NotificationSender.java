package com.example.projectwork.domain.notification.sender;

/**
 * 주문 완료 알림 발송 채널의 추상화. 이 인터페이스가 확장 지점이며,
 * 실제 발송 수단(예: KakaoTalk)은 구현체를 갈아끼워 대체한다.
 */
public interface NotificationSender {

	void send(Long memberId, String message);
}
