package dsn.webmail.service;

import dsn.webmail.entity.LearningResource;
import dsn.webmail.entity.TechKeyword;
import dsn.webmail.repository.LearningResourceRepository;
import dsn.webmail.repository.TechKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// v2에서는 메일 기반 동적 생성으로 변경되어 비활성화
// @Component
@Slf4j
@RequiredArgsConstructor
public class StudyDataInitializer implements CommandLineRunner {

    private final TechKeywordRepository keywordRepository;
    private final LearningResourceRepository resourceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (keywordRepository.count() > 0) {
            log.info("Study data already initialized, skipping...");
            return;
        }

        log.info("Initializing study data...");
        initKeywords();
        initResources();
        log.info("Study data initialization completed");
    }

    private void initKeywords() {
        List<TechKeyword> keywords = List.of(
            // Backend
            keyword("Spring Boot", "Backend", "자바 기반 웹 애플리케이션 프레임워크", "fab fa-java"),
            keyword("JPA", "Backend", "Java Persistence API - ORM 표준", "fas fa-database"),
            keyword("JWT", "Backend", "JSON Web Token - 토큰 기반 인증", "fas fa-key"),
            keyword("REST API", "Backend", "RESTful 웹 서비스 설계", "fas fa-exchange-alt"),
            keyword("Spring Security", "Backend", "Spring 보안 프레임워크", "fas fa-shield-alt"),
            keyword("Gradle", "Backend", "빌드 자동화 도구", "fas fa-cogs"),
            keyword("Maven", "Backend", "프로젝트 관리 및 빌드 도구", "fas fa-cubes"),
            keyword("MyBatis", "Backend", "SQL 매핑 프레임워크", "fas fa-code"),
            keyword("Hibernate", "Backend", "ORM 프레임워크", "fas fa-database"),
            keyword("Kafka", "Backend", "분산 스트리밍 플랫폼", "fas fa-stream"),

            // Frontend
            keyword("React", "Frontend", "UI 라이브러리", "fab fa-react"),
            keyword("Vue.js", "Frontend", "프로그레시브 프레임워크", "fab fa-vuejs"),
            keyword("TypeScript", "Frontend", "정적 타입 자바스크립트", "fab fa-js"),
            keyword("Thymeleaf", "Frontend", "서버사이드 템플릿 엔진", "fas fa-leaf"),
            keyword("JavaScript", "Frontend", "웹 프로그래밍 언어", "fab fa-js-square"),
            keyword("HTML", "Frontend", "마크업 언어", "fab fa-html5"),
            keyword("CSS", "Frontend", "스타일시트 언어", "fab fa-css3-alt"),
            keyword("Next.js", "Frontend", "React 프레임워크", "fas fa-forward"),
            keyword("Tailwind CSS", "Frontend", "유틸리티 퍼스트 CSS", "fas fa-paint-brush"),

            // Database
            keyword("MySQL", "Database", "관계형 데이터베이스", "fas fa-database"),
            keyword("PostgreSQL", "Database", "객체 관계형 데이터베이스", "fas fa-database"),
            keyword("Redis", "Database", "인메모리 데이터 저장소", "fas fa-memory"),
            keyword("MongoDB", "Database", "NoSQL 문서 데이터베이스", "fas fa-leaf"),
            keyword("Elasticsearch", "Database", "검색 및 분석 엔진", "fas fa-search"),

            // DevOps
            keyword("Docker", "DevOps", "컨테이너 플랫폼", "fab fa-docker"),
            keyword("Kubernetes", "DevOps", "컨테이너 오케스트레이션", "fas fa-dharmachakra"),
            keyword("Jenkins", "DevOps", "CI/CD 자동화 서버", "fas fa-cog"),
            keyword("GitHub Actions", "DevOps", "GitHub CI/CD", "fab fa-github"),
            keyword("AWS", "DevOps", "아마존 웹 서비스", "fab fa-aws"),
            keyword("Linux", "DevOps", "운영 체제", "fab fa-linux"),
            keyword("Nginx", "DevOps", "웹 서버", "fas fa-server"),
            keyword("Git", "DevOps", "버전 관리 시스템", "fab fa-git-alt")
        );

        keywordRepository.saveAll(keywords);
        log.info("Initialized {} tech keywords", keywords.size());
    }

    private void initResources() {
        // Spring Boot
        TechKeyword springBoot = keywordRepository.findByKeyword("Spring Boot").orElseThrow();
        saveResources(springBoot, List.of(
            resource("Spring Boot Reference Documentation", "https://spring.io/projects/spring-boot", LearningResource.ResourceType.OFFICIAL_DOC, "Spring.io", "en", 2, "Spring Boot 공식 레퍼런스", true),
            resource("Spring Boot 시작하기", "https://www.baeldung.com/spring-boot", LearningResource.ResourceType.BLOG, "Baeldung", "en", 1, "Spring Boot 입문 가이드", true),
            resource("Spring Boot 애플리케이션 성능 튜닝", "https://techblog.woowahan.com/", LearningResource.ResourceType.BLOG, "우아한형제들", "ko", 3, "실무 성능 최적화 사례", false)
        ));

        // JWT
        TechKeyword jwt = keywordRepository.findByKeyword("JWT").orElseThrow();
        saveResources(jwt, List.of(
            resource("JWT Introduction", "https://jwt.io/introduction", LearningResource.ResourceType.OFFICIAL_DOC, "jwt.io", "en", 1, "JWT 기본 개념과 구조", true),
            resource("Spring Security JWT 인증", "https://www.baeldung.com/spring-security-oauth-jwt", LearningResource.ResourceType.BLOG, "Baeldung", "en", 2, "Spring Security와 JWT 구현", true),
            resource("JWT를 소개합니다", "https://techblog.woowahan.com/", LearningResource.ResourceType.BLOG, "우아한형제들", "ko", 1, "JWT 개념 쉽게 이해하기", false)
        ));

        // Docker
        TechKeyword docker = keywordRepository.findByKeyword("Docker").orElseThrow();
        saveResources(docker, List.of(
            resource("Docker Documentation", "https://docs.docker.com/", LearningResource.ResourceType.OFFICIAL_DOC, "Docker", "en", 1, "Docker 공식 문서", true),
            resource("초보를 위한 도커 안내서", "https://subicura.com/2017/01/19/docker-guide-for-beginners-1.html", LearningResource.ResourceType.BLOG, "44bits", "ko", 1, "Docker 입문 가이드", true),
            resource("Docker Compose 실전 가이드", "https://meetup.toast.com/", LearningResource.ResourceType.BLOG, "NHN Cloud", "ko", 2, "Docker Compose 활용법", false)
        ));

        // React
        TechKeyword react = keywordRepository.findByKeyword("React").orElseThrow();
        saveResources(react, List.of(
            resource("React Documentation", "https://react.dev/", LearningResource.ResourceType.OFFICIAL_DOC, "React", "en", 1, "React 공식 문서", true),
            resource("React 완벽 가이드", "https://react.dev/learn", LearningResource.ResourceType.TUTORIAL, "React", "en", 2, "React 학습 튜토리얼", true)
        ));

        // MySQL
        TechKeyword mysql = keywordRepository.findByKeyword("MySQL").orElseThrow();
        saveResources(mysql, List.of(
            resource("MySQL Documentation", "https://dev.mysql.com/doc/", LearningResource.ResourceType.OFFICIAL_DOC, "MySQL", "en", 2, "MySQL 공식 문서", true),
            resource("MySQL 성능 최적화", "https://d2.naver.com/", LearningResource.ResourceType.BLOG, "네이버 D2", "ko", 3, "MySQL 쿼리 최적화", false)
        ));

        // Redis
        TechKeyword redis = keywordRepository.findByKeyword("Redis").orElseThrow();
        saveResources(redis, List.of(
            resource("Redis Documentation", "https://redis.io/docs/", LearningResource.ResourceType.OFFICIAL_DOC, "Redis", "en", 2, "Redis 공식 문서", true),
            resource("Redis 캐시 전략", "https://techblog.woowahan.com/", LearningResource.ResourceType.BLOG, "우아한형제들", "ko", 2, "Redis 캐싱 패턴", false)
        ));

        // Kubernetes
        TechKeyword k8s = keywordRepository.findByKeyword("Kubernetes").orElseThrow();
        saveResources(k8s, List.of(
            resource("Kubernetes Documentation", "https://kubernetes.io/docs/", LearningResource.ResourceType.OFFICIAL_DOC, "Kubernetes", "en", 3, "Kubernetes 공식 문서", true),
            resource("Kubernetes 입문", "https://subicura.com/k8s/", LearningResource.ResourceType.TUTORIAL, "44bits", "ko", 2, "Kubernetes 시작하기", true)
        ));

        // TypeScript
        TechKeyword ts = keywordRepository.findByKeyword("TypeScript").orElseThrow();
        saveResources(ts, List.of(
            resource("TypeScript Handbook", "https://www.typescriptlang.org/docs/", LearningResource.ResourceType.OFFICIAL_DOC, "TypeScript", "en", 2, "TypeScript 핸드북", true),
            resource("TypeScript 입문", "https://joshua1988.github.io/ts/", LearningResource.ResourceType.TUTORIAL, "캡틴판교", "ko", 1, "TypeScript 시작하기", true)
        ));

        // Git
        TechKeyword git = keywordRepository.findByKeyword("Git").orElseThrow();
        saveResources(git, List.of(
            resource("Pro Git Book", "https://git-scm.com/book/ko/v2", LearningResource.ResourceType.OFFICIAL_DOC, "Git", "ko", 2, "Git 공식 가이드북", true),
            resource("Git 브랜치 전략", "https://techblog.woowahan.com/", LearningResource.ResourceType.BLOG, "우아한형제들", "ko", 2, "Git Flow vs GitHub Flow", false)
        ));

        log.info("Initialized learning resources");
    }

    private TechKeyword keyword(String name, String category, String description, String iconClass) {
        return TechKeyword.builder()
                .keyword(name)
                .category(category)
                .description(description)
                .iconClass(iconClass)
                .build();
    }

    private LearningResource resource(String title, String url, LearningResource.ResourceType type,
            String source, String language, Integer difficulty, String summary, Boolean isRecommended) {
        return LearningResource.builder()
                .title(title)
                .url(url)
                .type(type)
                .source(source)
                .language(language)
                .difficulty(difficulty)
                .summary(summary)
                .isRecommended(isRecommended)
                .build();
    }

    private void saveResources(TechKeyword keyword, List<LearningResource> resources) {
        resources.forEach(r -> {
            r.setKeyword(keyword);
            resourceRepository.save(r);
        });
    }
}
