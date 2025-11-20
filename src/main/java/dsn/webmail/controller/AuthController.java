package dsn.webmail.controller;

import dsn.webmail.dto.AuthDtos.*;
import dsn.webmail.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "로그인", description = "JWT 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.email(), request.password());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "클라이언트에서 토큰을 삭제하세요.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT는 Stateless이므로 서버에서 할 일 없음
        // 클라이언트에서 토큰 삭제
        // 향후: Refresh Token을 DB에 저장하고 여기서 삭제 가능
        return ResponseEntity.ok().build();
    }
}
