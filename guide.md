# server-spring — 사보원 오프라인 셋업 가이드

## 상태: 빌드는 이미 끝나 있음

`build/libs/CallWrap_Spring-0.0.1-SNAPSHOT.jar`가 이미 만들어져 있다.
**`./gradlew build`를 새로 돌릴 필요 없다** — JAR을 바로 실행하면 된다.

## 필요한 것

- **Docker Desktop** — Postgres·RabbitMQ·MinIO를 띄우는 용도. 없으면 설치부터.
  - WSL2 백엔드가 필요하다. 이 머신에 WSL2가 없으면 관리자 권한으로 `wsl --install` → 재부팅 → Docker Desktop 실행.
- **Java 21** — `java -version`으로 확인. 21이 아니면 번들된 `jdk21-windows-x64.zip`을 쓴다:
  ```bash
  unzip jdk21-windows-x64.zip -d jdk21
  ```

## 실행 순서

### 1. 인프라
```bash
docker compose up -d postgres rabbitmq minio minio-init
```
`docker compose ps`로 RabbitMQ가 `healthy`가 될 때까지 기다린다.

### 2. Spring
```bash
# 번들된 JDK21을 쓰는 경우
./jdk21/*/bin/java -jar build/libs/CallWrap_Spring-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
# 시스템에 이미 Java 21이 있으면
java -jar build/libs/CallWrap_Spring-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```
`Started CallWrapApplication`이 뜨면 정상. Flyway가 DB 스키마를 자동으로 만든다.

### 3. 테스트용 상담사 계정 생성 (최초 1회)
`calls` 테이블이 `counselors`를 FK로 참조해서, 존재하지 않는 counselorId로 리뷰를 만들면
500 에러가 난다:
```bash
docker exec callwrap-spring-postgres psql -U callwrap -d callwrap -c "
INSERT INTO counselors (id, login_id, password_hash, name)
VALUES ('c0000000-0000-0000-0000-000000000001', 'tester', '\$2b\$12\$4ZrmF/X6jRxkZhmKJMSn4.wWFmj8OV23mTJD6edlwmdNxIgcchbo6', '테스터')
ON CONFLICT (id) DO NOTHING;"
```
로그인 정보: 아이디 `tester`, 비밀번호 `test1234`.

## 확인

| 서비스 | 주소 |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ 관리 콘솔 | http://localhost:15672 (guest/guest) |

`curl http://localhost:8080/api/reviews/stats` 가 에러 없이 JSON을 돌려주면 정상
(이전에 `hourlyThroughput`/`maliciousByCounselor` 필드 누락으로 관리자 대시보드가
죽던 버그가 있었는데, 이 JAR에는 수정되어 있다).

## 참고

- FastAPI(`server-fastapi/`)가 이 서비스의 `POST /api/internal/faq/*`를 호출하므로,
  FAQ 배치 생성까지 테스트하려면 FastAPI도 같이 떠 있어야 한다 — [server-fastapi/guide.md](../server-fastapi/guide.md) 참고.
- 전체 조합 실행 순서는 루트의 `사보원_테스트_가이드.md`를 따른다.
