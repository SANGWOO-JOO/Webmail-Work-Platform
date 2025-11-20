package dsn.webmail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    @Schema(description = "로그인 요청")
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }


    @Schema(description = "로그인 응답")
    public record LoginResponse(
            @Schema(description = "Access Token")
            String accessToken,

            @Schema(description = "Refresh Token")
            String refreshToken,

            @Schema(description = "토큰 타입", example = "Bearer")
            String tokenType,

            @Schema(description = "Access Token 만료 시간 (초)", example = "1800")
            Long expiresIn,

            @Schema(description = "사용자 이메일")
            String email
    ) {
    }

    @Schema(description = "토큰 갱신 요청")
    public record RefreshTokenRequest(
            @NotBlank String refreshToken
    ) {
    }

    @Schema(description = "토큰 갱신 응답")
    public record RefreshTokenResponse(
            @Schema(description = "새 Access Token")
            String accessToken,

            @Schema(description = "토큰 타입")
            String tokenType,

            @Schema(description = "만료 시간 (초)")
            Long expiresIn
    ) {
    }

}
