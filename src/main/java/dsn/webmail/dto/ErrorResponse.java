package dsn.webmail.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "표준 에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 코드(비즈니스 식별자)", example = "AUTH-001")
        String code,
        @Schema(description = "에러 메시지(사용자 노출)", example = "코드가 만료되었습니다.")
        String message,
        @Schema(description = "개발 디버깅용 상세", example = "expiredAt=2025-08-25T10:12:00+09:00")
        String detail
) {
    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}