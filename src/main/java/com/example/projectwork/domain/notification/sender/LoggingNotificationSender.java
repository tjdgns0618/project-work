package com.example.projectwork.domain.notification.sender;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 알림 발송 Mock 구현 — 외부 API·시크릿 없이 로그만 남긴다.
 * 실제 채널(예: KakaoTalk)을 붙일 때 이 빈을 다른 {@link NotificationSender} 구현으로 대체한다.
 */
@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {

	@Override
	public void send(Long memberId, String message) {
		log.info("[알림] memberId={} : {}", memberId, message);
	}
}
