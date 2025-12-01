# Webmail - 사내 업무 통합 플랫폼

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white"/>
  <img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white"/>
</p>

## 프로젝트 소개

**Webmail**은 사내 이메일 시스템과 연동하여 업무 효율성을 높이는 통합 플랫폼입니다.

POP3 프로토콜을 통해 메일을 수신하고, AI(LLM)를 활용하여 메일 분류/요약, 일정 추출, 답장 초안 생성 등의 스마트 기능을 제공합니다. 또한 맛집 지도, AI 음악 추천 등 사내 복지 기능도 함께 제공합니다.

### 개발 동기

- 사내 메일 확인을 위해 별도 클라이언트를 열어야 하는 불편함 해소
- 메일에서 일정/회의 정보를 수동으로 캘린더에 옮기는 반복 작업 자동화
- 점심시간 맛집 결정에 소요되는 시간 단축
- LLM 활용 실무 경험 축적

---

## 주요 기능

### 1. 메일 관리
| 기능 | 설명 |
|------|------|
| POP3 폴링 | 30초 간격으로 사용자 메일함 모니터링 |
| AI 메일 분류 | 공지, 노션 알림, 업무 요청, 내용 전달, 질의 사항 5가지 카테고리 자동 분류 |
| AI 메일 요약 | 메일 내용을 핵심 5줄로 요약 |
| AI 답장 초안 | 메일 내용 분석 후 톤(Professional, Friendly 등) 선택 가능한 답장 초안 자동 생성 |
| Slack 알림 | 새 메일 도착 시 개인 DM 발송 |

### 2. 일정 관리
| 기능 | 설명 |
|------|------|
| AI 일정 추출 | 메일 본문에서 회의/일정 정보 자동 추출 |
| 달력 UI | 월별 달력에서 일정 확인 |
| 공휴일 API 연동 | 한국 공휴일 자동 표시 |
| 수동 일정 생성 | 직접 일정 생성/수정/삭제 |
| 색상 선택 | 일정별 색상 지정 |

### 3. 맛집 지도
| 기능 | 설명 |
|------|------|
| 주변 맛집 검색 | 사내(도원빌딩) 기준 500m~1km 반경 맛집 표시 |
| Kakao Map 연동 | 지도에 마커로 식당 위치 표시 |
| 카테고리 필터 | 대분류/중분류/소분류 3단계 계층 필터 |
| 좋아요/싫어요 | 팀원 평가 기능 |
| 즐겨찾기 | 자주 가는 맛집 저장 |
| 방문 기록 | 방문 일시 및 메모 기록 |

### 4. AI 음악 추천
| 기능 | 설명 |
|------|------|
| 기분 기반 추천 | 사용자 입력 분석 후 맞춤 음악 추천 |
| 메일 기반 추천 | 오늘 받은 메일 분위기 분석 후 힐링 음악 추천 |
| Spotify 연동 | Spotify Web API로 실제 음악 검색/재생 |
| 필터 검색 | 아티스트, 장르, 분위기, 국가별 검색 |

### 5. 학습 자료 추천
| 기능 | 설명 |
|------|------|
| 키워드 기반 추천 | 메일/일정에서 추출한 기술 키워드로 학습 자료 추천 |
| 공식 문서 + 블로그 | 다양한 소스의 학습 자료 통합 제공 |

---

## 기술 스택

### Backend
| 기술 | 버전 | 용도 |
|------|------|------|
| Spring Boot | 3.5.5 | 웹 프레임워크 |
| Java | 21 | 언어 |
| Spring Security | 6.x | 인증/인가 (JWT) |
| Spring Data JPA | - | ORM |
| Spring Mail | - | SMTP/POP3 메일 처리 |
| Langchain4j | - | LLM 통합 (OpenAI gpt-4o-mini) |
| Jasypt | - | 비밀번호 암호화 |

### Frontend
| 기술 | 용도 |
|------|------|
| Thymeleaf | 서버사이드 템플릿 |
| Bootstrap 5.1.3 | UI 프레임워크 |
| Font Awesome 6.0.0 | 아이콘 |
| Kakao Map API | 지도 표시 |

### Database
| 기술 | 용도 |
|------|------|
| MariaDB 11.2 | 운영 데이터베이스 |
| H2 | 개발/테스트용 인메모리 DB |

### Infrastructure
| 기술 | 용도 |
|------|------|
| Docker | 컨테이너 (Multi-stage build) |
| GitHub Actions | CI/CD |
| AWS EC2 + ECR | 클라우드 배포 |

### External APIs
| API | 용도 |
|-----|------|
| OpenAI GPT (gpt-4o-mini) | 메일 분류/요약, 일정 추출, 답장 생성 |
| Spotify Web API | 음악 검색/추천 |
| Kakao Local API | 주변 맛집 검색 |
| 공공데이터 공휴일 API | 공휴일 정보 |
| Slack Bot API | 새 메일 알림 |

---

## 시스템 아키텍처

<p align="center">
  <img src="./picture/webmail%20아키텍처.svg" alt="System Architecture" width="100%"/>
</p>

---

## 프로젝트 구조

```
src/main/java/dsn/webmail/
├── config/              # 설정 클래스
│   ├── SecurityConfig.java      # Spring Security + JWT 설정
│   ├── AsyncConfig.java         # 비동기 처리 설정
│   └── Langchain4jConfig.java   # LLM 설정
├── controller/          # REST API 컨트롤러
│   ├── AuthController.java      # 인증 API
│   ├── MailController.java      # 메일 API
│   ├── ScheduleController.java  # 일정 API
│   ├── MapController.java       # 맛집 지도 API
│   └── MusicController.java     # 음악 추천 API
├── service/             # 비즈니스 로직
│   ├── MailAnalyzerService.java     # 메일 분석 (AI)
│   ├── MailAnalysisAiService.java   # LLM 호출
│   ├── EventExtractionService.java  # 일정 추출 (AI)
│   ├── MapService.java              # 맛집 서비스
│   └── MusicRecommendationService.java  # 음악 추천
├── entity/              # JPA 엔티티
│   ├── AppUser.java
│   ├── ProcessedMail.java
│   ├── MailEvent.java
│   └── Restaurant.java
├── repository/          # JPA 레포지토리
├── dto/                 # 데이터 전송 객체
├── security/            # JWT 관련
│   ├── JwtTokenProvider.java
│   └── JwtAuthenticationFilter.java
├── scheduler/           # 스케줄링 작업
│   ├── MultiUserMailScheduler.java
│   └── RestaurantSyncScheduler.java
└── exception/           # 예외 처리

src/main/resources/
├── templates/           # Thymeleaf 템플릿
│   ├── dashboard/       # 대시보드
│   ├── mail/            # 메일
│   ├── schedule/        # 일정
│   ├── map/             # 맛집 지도
│   └── music/           # 음악 추천
└── static/              # CSS, JS
```

---

## 설치 및 실행

### 사전 요구사항
- Java 21+
- MariaDB 11.2+
- Gradle 8.0+

### 환경 변수 설정

`.env` 파일 생성:

```bash
# Database
MARIADB_ROOT_PASSWORD=<password>
MARIADB_DATABASE=webmail
MARIADB_USER=<user>
MARIADB_PASSWORD=<password>

# Security
JASYPT_PASSWORD=<encryption-key>

# Mail (POP3)
MAIL_POP3_HOST=<pop3-server>
MAIL_POP3_PORT=995

# AI
OPENAI_API_KEY=<openai-key>

# Slack
SLACK_BOT_TOKEN=xoxb-<token>

# Spotify
SPOTIFY_CLIENT_ID=<spotify-id>
SPOTIFY_CLIENT_SECRET=<spotify-secret>

# Kakao
KAKAO_REST_API_KEY=<kakao-api-key>
```

### 로컬 실행

```bash
# 1. 프로젝트 클론
git clone https://github.com/SANGWOO-JOO/webmail.git
cd webmail

# 2. 빌드
./gradlew build

# 3. 실행
./gradlew bootRun

# 4. 접속
# http://localhost:8080
```

### Docker 실행

```bash
# Docker Compose로 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f webmail
```

---

## API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/auth/login` | JWT 토큰 발급 |
| POST | `/auth/refresh` | 토큰 갱신 |
| POST | `/api/signup/request-code` | 회원가입 인증 코드 요청 |
| POST | `/api/signup/verify` | 인증 코드 검증 |
| GET | `/api/mail` | 메일 목록 조회 |
| POST | `/api/mail/{id}/analyze` | AI 메일 분석 |
| POST | `/api/mail/{id}/reply-draft` | AI 답장 초안 생성 |
| GET | `/api/schedule` | 일정 목록 조회 |
| POST | `/api/schedule` | 일정 생성 |
| GET | `/map/api/restaurants` | 맛집 목록 조회 |
| POST | `/map/api/restaurants/{id}/reaction` | 좋아요/싫어요 |
| POST | `/music/api/recommend` | AI 음악 추천 |

### API 문서

애플리케이션 실행 후 Swagger UI에서 API 문서 확인:
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

---

## 화면 구성

| 페이지 | 경로 | 설명 |
|--------|------|------|
| 로그인 | `/login` | JWT 기반 로그인 |
| 회원가입 | `/signup` | 이메일 인증 기반 회원가입 |
| 대시보드 | `/dashboard` | 메인 대시보드 |
| 메일 | `/mail` | 메일 목록 및 상세 |
| 일정 | `/schedule` | 달력 기반 일정 관리 |
| 맛집 지도 | `/map` | 주변 맛집 검색 |
| 음악 | `/music` | AI 음악 추천 |

---

## 배포

### GitHub Actions CI/CD

`master` 브랜치에 push 시 자동 배포:

1. Gradle 빌드
2. Docker 이미지 빌드 (Multi-stage)
3. AWS ECR에 push
4. EC2에서 docker-compose로 배포

### GitHub Secrets 설정

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`

---

## 개발 기간

- **시작일**: 2024년 8월
- **현재 상태**: 개발 중

---

## 향후 계획

- [ ] 메일 답장 기능 (SMTP 발송)
- [ ] 팀원 간 일정 공유
- [ ] 모바일 반응형 UI 개선
- [ ] Redis 캐싱 적용
- [ ] 테스트 코드 보강

---

## 라이선스

Private Repository
