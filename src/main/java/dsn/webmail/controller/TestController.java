package dsn.webmail.controller;

import dsn.webmail.entity.AppUser;
import dsn.webmail.repository.AppUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Test", description = "테스트 API")
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TestController(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(
        summary = "공개 엔드포인트",
        description = "인증 없이 접근 가능한 테스트 엔드포인트"
    )
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a public endpoint");
        response.put("authenticated", false);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "보호된 엔드포인트",
        description = "인증이 필요한 테스트 엔드포인트",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/protected")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> protectedEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a protected endpoint");
        response.put("authenticated", true);
        response.put("user", authentication.getName());
        response.put("authorities", authentication.getAuthorities());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "관리자 전용 엔드포인트",
        description = "ADMIN 권한이 필요한 테스트 엔드포인트",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is an admin-only endpoint");
        response.put("authenticated", true);
        response.put("user", authentication.getName());
        response.put("authorities", authentication.getAuthorities());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "테스트 사용자 생성",
        description = "로그인 테스트를 위한 사용자 생성 (개발 전용)"
    )
    @PostMapping("/create-test-user")
    public ResponseEntity<Map<String, Object>> createTestUser() {
        String email = "test@example.com";
        String password = "password123";

        // 기존 사용자 삭제
        userRepository.findByEmail(email).ifPresent(user -> userRepository.delete(user));

        // 새 사용자 생성
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEncryptedPop3Password("test_pop3_password");
        user.setStatus(AppUser.Status.ACTIVE);
        user.setRole(AppUser.Role.USER);
        user.setWebLoginEnabled(true);
        user.setMailPollingEnabled(false);
        user.setLoginFailureCount(0);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Test user created successfully");
        response.put("email", email);
        response.put("password", password);
        response.put("status", "ACTIVE");

        return ResponseEntity.ok(response);
    }
}
