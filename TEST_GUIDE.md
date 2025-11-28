# StockKnock 테스트 가이드

이 문서는 StockKnock 프로젝트를 로컬 환경에서 테스트하는 방법을 안내합니다.

## 1단계: 데이터베이스 설정

### PostgreSQL 데이터베이스 및 사용자 생성

자세한 설정은 [POSTGRESQL_SETUP.md](./POSTGRESQL_SETUP.md)를 참고하세요.

터미널에서 다음 명령어를 실행하세요:

```bash
psql postgres
```

PostgreSQL에 접속한 후 다음 SQL 명령어를 실행하세요:

```sql
-- 데이터베이스 생성
CREATE DATABASE stockknockDB;

-- 사용자 생성
CREATE USER sxxm WITH PASSWORD 'sxxmpass';

-- 권한 부여
GRANT ALL PRIVILEGES ON DATABASE stockknockDB TO sxxm;
ALTER DATABASE stockknockDB OWNER TO sxxm;

-- PostgreSQL 15+ 사용 시 추가 권한
\c stockknockDB
GRANT ALL ON SCHEMA public TO sxxm;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO sxxm;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO sxxm;

-- 확인
\l
\du
```

`\q` 명령어로 PostgreSQL에서 나옵니다.

---

## 2단계: 백엔드 설정 및 실행

### 2-1. 백엔드 디렉토리로 이동

```bash
cd knockBE
```

### 2-2. Gradle 래퍼 권한 설정 (처음 한 번만)

```bash
chmod +x gradlew
```

### 2-3. 프로젝트 빌드

```bash
./gradlew clean build
```

### 2-4. 백엔드 실행

```bash
./gradlew bootRun
```

백엔드가 성공적으로 실행되면 다음과 같은 메시지가 보입니다:
```
Started StockknockApplication in X.XXX seconds
```

백엔드 서버는 `http://localhost:8080`에서 실행됩니다.

---

## 3단계: 프론트엔드 설정 및 실행

### 3-1. 새로운 터미널 창 열기

백엔드가 실행 중인 터미널은 그대로 두고, **새로운 터미널 창**을 엽니다.

### 3-2. 프론트엔드 디렉토리로 이동

```bash
cd /Users/sxxm/Documents/GitHub/stockknock/knockFE
```

### 3-3. 의존성 설치 (처음 한 번만)

```bash
npm install
```

### 3-4. 프론트엔드 실행

```bash
npm run dev
```

프론트엔드가 성공적으로 실행되면 다음과 같은 메시지가 보입니다:
```
VITE vX.X.X  ready in XXX ms

➜  Local:   http://localhost:5173/
```

> **참고**: Vite는 기본적으로 포트 5173을 사용합니다. 포트 3000을 사용하려면 `vite.config.ts` 파일을 수정해야 합니다.

---

## 4단계: 애플리케이션 테스트

### 4-1. 브라우저에서 접속

프론트엔드 실행 메시지에 표시된 URL로 접속하세요 (예: `http://localhost:5173`).

### 4-2. 회원가입 및 로그인

1. 로그인 페이지가 표시됩니다.
2. "계정이 없으신가요? 회원가입"을 클릭합니다.
3. 이메일과 비밀번호를 입력하고 회원가입합니다.
4. 자동으로 로그인되어 대시보드로 이동합니다.

### 4-3. 기능 테스트

#### 기본 테스트 시나리오:

1. **대시보드 확인**
   - 포트폴리오 요약 확인
   - 최근 뉴스 확인

2. **포트폴리오 추가** (테스트용 데이터 필요)
   - 포트폴리오 페이지로 이동
   - 주식 종목을 추가하려면 먼저 주식 데이터가 필요합니다

3. **뉴스 확인**
   - 뉴스 페이지로 이동
   - 뉴스 목록 확인 (데이터가 없으면 빈 목록)

4. **AI 채팅 테스트**
   - AI 채팅 페이지로 이동
   - 질문 입력 (예: "애플 주식의 전망은 어때요?")
   - AI 응답 확인 (OpenAI API Key가 설정되어 있어야 함)

---

## 5단계: API 직접 테스트 (선택사항)

### Postman 또는 curl 사용

#### 회원가입 테스트

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "test1234"
  }'
```

#### 로그인 테스트

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "test1234"
  }'
```

응답에서 받은 `token`을 저장해두세요.

#### 포트폴리오 조회 (인증 필요)

```bash
curl -X GET http://localhost:8080/api/portfolio \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 문제 해결

### 백엔드 실행 오류

#### PostgreSQL 연결 오류
```
Could not connect to PostgreSQL server
```
- PostgreSQL 서버가 실행 중인지 확인: `psql -U sxxm -d stockknockDB -h localhost`로 접속 테스트
- 데이터베이스와 사용자가 올바르게 생성되었는지 확인
- PostgreSQL 인증 설정(`pg_hba.conf`) 확인

#### 포트 8080이 이미 사용 중
```
Port 8080 is already in use
```
- 다른 애플리케이션이 포트 8080을 사용 중일 수 있습니다.
- `application.properties`에서 `server.port=8081`로 변경하거나
- 실행 중인 프로세스 종료: `lsof -ti:8080 | xargs kill`

### 프론트엔드 실행 오류

#### 포트 5173이 이미 사용 중
- Vite가 자동으로 다른 포트를 선택합니다.
- 또는 `vite.config.ts`에서 포트 변경:
```typescript
export default defineConfig({
  server: {
    port: 3000
  }
})
```

#### CORS 오류
- 백엔드의 `application.properties`에서 CORS 설정 확인
- 프론트엔드 포트가 허용된 포트 목록에 있는지 확인

### AI 기능이 작동하지 않음

OpenAI API Key가 설정되지 않았거나 유효하지 않은 경우:
- `application.properties`에서 API Key 설정 확인
- 환경 변수 `OPENAI_API_KEY` 설정

**참고**: OpenAI API Key 없이도 다른 기능들은 정상 작동합니다.

---

## 테스트 데이터 추가 (선택사항)

실제 주식 데이터를 테스트하려면:

1. MySQL에 직접 데이터 삽입
2. 또는 백엔드에서 샘플 데이터 생성 스크립트 작성

---

## 다음 단계

- [ ] 실제 주식 데이터 연동 (외부 API)
- [ ] 뉴스 데이터 자동 수집 스케줄러 구현
- [ ] 실시간 주가 업데이트 기능
- [ ] 포트폴리오 리포트 생성 기능

---

## 개발 모드 팁

### 백엔드 로그 확인
백엔드 실행 시 콘솔에 SQL 쿼리와 요청 로그가 표시됩니다.

### 프론트엔드 Hot Reload
프론트엔드 코드를 수정하면 자동으로 브라우저가 새로고침됩니다.

### 데이터베이스 확인
```bash
psql -U sxxm -d stockknockDB -h localhost
```
PostgreSQL 콘솔에서:
```sql
\dt
SELECT * FROM users;
\q
```

---

**질문이나 문제가 있으면 로그를 확인하거나 이슈를 등록해주세요!**

