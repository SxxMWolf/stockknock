# PostgreSQL로 변경 완료

MySQL에서 PostgreSQL로 데이터베이스를 변경했습니다.

## 변경된 파일

### 1. `knockBE/build.gradle`
- ✅ MySQL 드라이버 제거: `com.mysql:mysql-connector-j`
- ✅ PostgreSQL 드라이버 추가: `org.postgresql:postgresql`

### 2. `knockBE/src/main/resources/application.properties`
- ✅ 데이터베이스 URL: `jdbc:postgresql://localhost:5432/stockknockDB`
- ✅ 드라이버 클래스: `org.postgresql.Driver`
- ✅ Hibernate Dialect: `org.hibernate.dialect.PostgreSQLDialect`
- ✅ PostgreSQL 전용 설정 추가

### 3. 문서 업데이트
- ✅ `README.md` - PostgreSQL로 업데이트
- ✅ `TEST_GUIDE.md` - PostgreSQL 설정 가이드 추가
- ✅ `POSTGRESQL_SETUP.md` - 상세한 PostgreSQL 설정 가이드 생성
- ✅ `quick_start.sh` - PostgreSQL 연결 확인으로 변경

## 다음 단계

### 1. PostgreSQL 데이터베이스 생성

```bash
psql postgres
```

PostgreSQL 콘솔에서:
```sql
CREATE DATABASE stockknockDB;
CREATE USER sxxm WITH PASSWORD 'sxxmpass';
GRANT ALL PRIVILEGES ON DATABASE stockknockDB TO sxxm;
ALTER DATABASE stockknockDB OWNER TO sxxm;

-- PostgreSQL 15+ 사용 시
\c stockknockDB
GRANT ALL ON SCHEMA public TO sxxm;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO sxxm;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO sxxm;
```

### 2. 백엔드 빌드 및 실행

```bash
cd knockBE
./gradlew clean build
./gradlew bootRun
```

### 3. 연결 테스트

다른 터미널에서:
```bash
psql -U sxxm -d stockknockDB -h localhost
```

비밀번호는 `sxxmpass`입니다.

## 자세한 설정 방법

상세한 설정과 문제 해결은 다음 문서를 참고하세요:
- [POSTGRESQL_SETUP.md](./POSTGRESQL_SETUP.md) - PostgreSQL 설정 가이드
- [TEST_GUIDE.md](./TEST_GUIDE.md) - 전체 테스트 가이드

