plugins {
    java
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dsn"
version = "0.0.1-SNAPSHOT"
description = "webmail"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
// --- Core ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // --- DB ---
//    runtimeOnly("com.h2database:h2") // dev/test
     runtimeOnly("org.mariadb.jdbc:mariadb-java-client") // 운영 DB

    // --- Security/Encryption ---
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5")
    implementation ("org.mariadb.jdbc:mariadb-java-client")
    // --- Mail ---
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // --- OpenAPI ---
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // --- Thymeleaf ---
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // --- Test ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // --- Utils & Tools ---
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation("org.jsoup:jsoup:1.17.2")  // HTML 파싱 및 텍스트 추출

    // JWT 라이브러리
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // LangChain4j - OpenAI 연동 (핵심!)
    implementation("dev.langchain4j:langchain4j:0.34.0")
    implementation("dev.langchain4j:langchain4j-open-ai:0.34.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
