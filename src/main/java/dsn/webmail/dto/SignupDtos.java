package dsn.webmail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupDtos {

    @Schema(description = "인증 코드 요청 DTO")
    public record RequestCode(
            @Schema(description = "가입 이메일", example = "johndoe@dsntech.com")
            @NotBlank(message = "이메일은 필수입니다") 
            @Email(message = "올바른 이메일 형식이 아닙니다") 
            String email,

            @Schema(description = "POP3 계정 비밀번호(양방향 암호 저장)", example = "any_password_string")
            @NotBlank(message = "POP3 비밀번호는 필수입니다") 
            @Size(min = 1, max = 255, message = "POP3 비밀번호는 1-255자 사이여야 합니다")
            String pop3Password
    ) {}

    @Schema(description = "코드 검증 DTO")
    public record VerifyCode(
            @Schema(description = "가입 이메일", example = "johndoe@dsntech.com")
            @NotBlank(message = "이메일은 필수입니다") 
            @Email(message = "올바른 이메일 형식이 아닙니다") 
            String email,

            @Schema(description = "6자리 인증 코드", example = "123456")
            @NotBlank(message = "인증 코드는 필수입니다") 
            @Pattern(regexp = "^[0-9]{6}$", message = "6자리 숫자만 허용됩니다")
            String code
    ) {}

    @Schema(description = "일반 메시지 응답")
    public record MessageResponse(
            @Schema(description = "메시지", example = "인증 코드가 발송되었습니다.")
            String message
    ) {}
}