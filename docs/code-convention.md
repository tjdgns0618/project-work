# 코드 컨벤션 (커피 주문 서비스 MVP)

> 출처: [team-11st-chat/11th-street Wiki — CodeConvention](https://github.com/team-11st-chat/11th-street/wiki/CodeConvention), [Code-Convention-Examples](https://github.com/team-11st-chat/11th-street/wiki/Code-Convention-Examples)
> WebSocket 관련 규칙은 이 프로젝트 범위 밖이라 제외했다. 예시는 본 프로젝트 도메인(`member`, `coffee`, `order`)에 맞춰 각색했다. ([erd.md](erd.md), [api-spec.md](api-spec.md) 참고)

## 패키지 구조

도메인 단위로 나누고, 공통 관심사는 `global`에 모은다.

- **global 패키지**: 공통 config, response, exception, security
- **도메인 패키지**: `member`, `coffee`, `order`
- 각 도메인은 자신의 Controller, Service, Repository, Entity, DTO, Exception을 관리한다.

## 클래스 네이밍

| 클래스 종류 | 네이밍 |
|---|---|
| Controller | `XxxController` |
| Service | `XxxService` |
| Repository | `XxxRepository` |
| Entity | `Xxx` |
| Request DTO | `XxxRequest` / `XxxCreateRequest` |
| Response DTO | `XxxResponse` |
| Mapper | `XxxMapper` |
| ErrorCode | `XxxErrorCode` |
| Exception | `XxxException` |

## 계층별 책임

### Controller
- 요청/응답 처리만 담당하고 비즈니스 로직은 Service에 위임한다.
- Entity를 그대로 반환하지 않고 Request/Response DTO를 사용한다.
- REST API는 공통 응답 포맷을 따른다.

### Service
- 비즈니스 로직과 도메인 흐름을 담당한다. (검증, 존재 확인, 권한 확인 등)
- 여러 도메인 흐름이 얽히면 ApplicationService/Facade로 조합한다.
- 트랜잭션 범위는 최소로 유지하고, 엔티티 상태 변경은 setter가 아니라 의미 있는 도메인 메서드로 처리한다.

### Repository
- 데이터 접근만 담당한다. 커스텀 쿼리는 별도 테스트로 검증한다.

## Entity 설계

- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — JPA 기본 생성자 보호
- `@AllArgsConstructor(access = AccessLevel.PRIVATE)` — 전체 필드 생성자 은닉
- 생성은 public 생성자가 아니라 **정적 팩토리 메서드** 또는 의미 있는 생성 메서드로 한다.
- 상태 변경은 의미 있는 도메인 메서드로만 한다. **`@Setter`는 사용하지 않는다.**

```java
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String passwordHash;
    private String name;
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
```

## Lombok 가이드

| 어노테이션 | 사용 |
|---|---|
| `@Getter` | 허용 |
| `@Setter` | Entity에서 사용 금지 |
| `@NoArgsConstructor(access = PROTECTED)` | JPA 기본 생성자 |
| `@AllArgsConstructor(access = PRIVATE)` | 전체 필드 생성자 |
| `@RequiredArgsConstructor` | 의존성 주입 |
| `@Builder` | 선택적으로 사용 |
| `@Data` | 사용 금지 |

## DTO 가이드

- Request/Response DTO는 Java `record`를 우선한다.
- Request DTO에는 Bean Validation 어노테이션을 붙일 수 있다.
- DTO 안에 복잡한 변환 로직을 넣지 않는다.
- Entity를 그대로 응답하지 않고 Response DTO로 변환한다.
- 단순 변환은 정적 `from()` 메서드, 복잡한 다중 객체 변환은 Mapper를 둔다.

```java
// Request DTO
public record MemberCreateRequest(
    @NotBlank String email,
    @NotBlank String password,
    @NotBlank String name
) {
}

// Response DTO
public record MemberResponse(
    Long id,
    String email,
    String name
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
            member.getId(),
            member.getEmail(),
            member.getName()
        );
    }
}
```

## REST API 컨벤션

- RESTful 리소스 지향 URL 설계를 따른다.
- 경로는 복수형을 쓰고 `/api`로 시작한다. (예: `/api/v1/members`, `/api/v1/coffees`, `/api/v1/orders`)
- 상태 변경은 적절한 HTTP 메서드로 표현한다.

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ApiResponse<MemberResponse> signUp(
            @Valid @RequestBody MemberCreateRequest request
    ) {
        return ApiResponse.success(memberService.signUp(request));
    }
}
```

## 공통 응답 포맷

- 성공/실패 응답에는 클라이언트가 필요한 값만 담는다.
- HTTP 상태 코드가 의미를 전달하므로, 응답 본문에 중복 표기하지 않는다.
- i18n을 하지 않으면 메시지 코드는 생략한다.
- 구조: 결과 설명은 `message`, 성공 페이로드는 `data`(실패 시 `null`).

## 예외 / ErrorCode 관리

- 공통 예외는 `global.exception`, 도메인 예외는 각 도메인의 `exception` 패키지에 둔다.
- ErrorCode는 실제 `HttpStatus`와 메시지를 핵심 요소로 갖는다.
- 상태 코드가 충분히 의미를 전달하면 별도 코드 문자열은 분리(생략)한다.
- 공통 예외와 도메인 예외를 한 enum에 섞지 않는다.

```java
// 나쁜 예 — 상태로 충분한데 코드 문자열 중복
INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL_500", "서버 내부 오류가 발생했습니다.")

// 좋은 예
INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.")
```

## 검증(Validation)

- 입력 형식 검증은 Request DTO에서 `@Valid`로 한다.
- 비즈니스 규칙 검증은 Service 또는 도메인 객체에서 한다.
- 검증 실패 응답은 global 예외 핸들러가 처리한다.

## 테스트

- 핵심 비즈니스 로직과 주요 예외 흐름을 우선한다.
- given/when/then 구조를 사용한다.
- 테스트 이름은 검증하는 동작/실패 조건을 드러낸다.
- 하나의 테스트는 하나의 동작/예외만 검증한다.
- 단순 getter/setter, Lombok 생성 코드는 테스트하지 않는다.
- 테스트에서 Entity 생성은 public 생성자가 아니라 정적 팩토리 메서드/픽스처를 사용한다.

**테스트 우선순위**
1. Service 핵심 비즈니스 로직
2. 예외 시나리오
3. Controller 요청/응답 및 검증 (Service는 mock)
4. Entity 도메인 메서드
5. Repository 커스텀 쿼리

```java
@Test
void 존재하지_않는_회원이면_예외가_발생한다() {
    // given
    Long memberId = 1L;
    given(memberRepository.findById(memberId))
            .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> memberService.chargePoint(memberId, 10000L))
            .isInstanceOf(ServiceException.class)
            .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
}
```

## 개발 원칙

- 이른 추상화보다 반복되는 패턴이 드러났을 때 대응한다.
- 가독성과 유지보수성을 우선한다.
- 기능을 작은 단위로 나누고 도메인 책임 분리를 먼저 고려한다.
