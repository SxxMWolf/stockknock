# PostgreSQL 설정 가이드

## 1단계: PostgreSQL 설치 확인

```bash
psql --version
```

PostgreSQL이 설치되어 있지 않다면:

### macOS (Homebrew)
```bash
brew install postgresql@16
brew services start postgresql@16
```

### Ubuntu/Debian
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

## 2단계: PostgreSQL 설정

### 2-1. PostgreSQL 접속

기본 superuser로 접속:
```bash
psql postgres
```

또는:
```bash
sudo -u postgres psql
```

### 2-2. 데이터베이스 및 사용자 생성

PostgreSQL 콘솔에서 다음 명령어 실행:

```sql
-- 데이터베이스 생성
CREATE DATABASE stockknockDB;

-- 사용자 생성
CREATE USER sxxm WITH PASSWORD 'sxxmpass';

-- 권한 부여
GRANT ALL PRIVILEGES ON DATABASE stockknockDB TO sxxm;

-- PostgreSQL 15 이상의 경우 추가 권한 필요
ALTER DATABASE stockknockDB OWNER TO sxxm;

-- 스키마 권한 부여 (PostgreSQL 15+)
\c stockknockDB
GRANT ALL ON SCHEMA public TO sxxm;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO sxxm;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO sxxm;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO sxxm;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO sxxm;
```

### 2-3. 연결 확인

```sql
-- 현재 연결된 데이터베이스 확인
SELECT current_database();

-- 사용자 목록 확인
\du

-- 데이터베이스 목록 확인
\l

-- 종료
\q
```

### 2-4. 외부 접속 테스트

새로운 터미널에서:

```bash
psql -U sxxm -d stockknockDB -h localhost
```

비밀번호 입력 프롬프트가 나오면 `sxxmpass`를 입력하세요.

## 3단계: pg_hba.conf 설정 (필요한 경우)

인증 오류가 발생하면 `pg_hba.conf` 파일을 수정해야 할 수 있습니다.

### 파일 위치 확인
```bash
psql -U postgres -c "SHOW hba_file;"
```

또는:
```bash
sudo find / -name pg_hba.conf 2>/dev/null
```

### 설정 변경

파일을 열어서 다음 줄 추가 또는 수정:
```
# TYPE  DATABASE        USER            ADDRESS                 METHOD
host    stockknockDB    sxxm            127.0.0.1/32            md5
host    stockknockDB    sxxm            ::1/128                 md5
```

변경 후 PostgreSQL 재시작:
```bash
# macOS
brew services restart postgresql@16

# Linux
sudo systemctl restart postgresql
```

## 4단계: 애플리케이션 실행

### 백엔드 실행

```bash
cd knockBE
./gradlew bootRun
```

정상적으로 연결되면 콘솔에 다음과 같은 메시지가 표시됩니다:
```
Hibernate: create table ...
```

### 테이블 확인

다른 터미널에서:
```bash
psql -U sxxm -d stockknockDB -h localhost
```

```sql
-- 테이블 목록 확인
\dt

-- 특정 테이블 구조 확인
\d users

-- 데이터 확인
SELECT * FROM users;
```

## 문제 해결

### 오류: "FATAL: password authentication failed"

**해결 방법:**
1. 사용자 비밀번호 확인
2. `pg_hba.conf` 설정 확인
3. PostgreSQL 재시작

### 오류: "permission denied for schema public"

**해결 방법:**
```sql
GRANT ALL ON SCHEMA public TO sxxm;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO sxxm;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO sxxm;
```

### 오류: "could not connect to server"

**해결 방법:**
1. PostgreSQL 서비스 실행 확인:
   ```bash
   # macOS
   brew services list
   
   # Linux
   sudo systemctl status postgresql
   ```

2. PostgreSQL 시작:
   ```bash
   # macOS
   brew services start postgresql@16
   
   # Linux
   sudo systemctl start postgresql
   ```

### 포트 확인

PostgreSQL 기본 포트는 5432입니다. 다른 포트를 사용하는 경우:

1. 포트 확인:
   ```bash
   sudo lsof -i -P | grep LISTEN | grep postgres
   ```

2. `application.properties`에서 포트 변경:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5433/stockknockDB
   ```

## 유용한 PostgreSQL 명령어

```sql
-- 현재 데이터베이스 확인
SELECT current_database();

-- 모든 테이블 목록
\dt

-- 테이블 구조 상세 보기
\d table_name

-- 모든 데이터베이스 목록
\l

-- 연결 정보 확인
\conninfo

-- 쿼리 실행 시간 표시
\timing

-- 종료
\q
```

## 성능 최적화 (선택사항)

```sql
-- 데이터베이스 통계 정보 수집
ANALYZE;

-- 인덱스 재구성
REINDEX DATABASE stockknockDB;

-- 연결 수 확인
SELECT count(*) FROM pg_stat_activity;
```

