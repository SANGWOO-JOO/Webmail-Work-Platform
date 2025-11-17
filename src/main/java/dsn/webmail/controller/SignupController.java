package dsn.webmail.controller;

import dsn.webmail.dto.ErrorResponse;
import dsn.webmail.dto.SignupDtos.MessageResponse;
import dsn.webmail.dto.SignupDtos.RequestCode;
import dsn.webmail.dto.SignupDtos.VerifyCode;
import dsn.webmail.service.SignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Signup", description = "회원가입 이메일 인증 API")
@RestController
@RequestMapping(value = "/api/signup", produces = MediaType.APPLICATION_JSON_VALUE)
public class SignupController {

    private final SignupService signupService;
    
    public SignupController(SignupService signupService) { 
        this.signupService = signupService; 
    }

    @Operation(
            summary = "인증 코드 발송",
            description = """
                    가입 절차 시작. 이메일로 6자리 인증 코드를 전송합니다.
                    - 코드 유효기간: 10분
                    - 재전송 쿨다운: 60초
                    - 기존 가입(ACTIVE) 시 409 반환
                    """
    )
    @ApiResponse(responseCode = "200", description = "발송 성공",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "검증 실패(형식/쿨다운 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "이미 가입된 이메일",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/request-code")
    public ResponseEntity<MessageResponse> requestCode(
            @RequestBody(description = "인증 코드 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RequestCode.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody RequestCode dto) {

        signupService.requestCode(dto.email(), dto.pop3Password());
        return ResponseEntity.ok(new MessageResponse("인증 코드가 발송되었습니다."));
    }

    @Operation(
            summary = "인증 코드 검증",
            description = """
                    이메일로 받은 6자리 코드를 검증합니다.
                    - 성공: 사용자 상태 ACTIVE 전환
                    - 실패: 시도 횟수 증가, 5회 초과 또는 만료 시 에러
                    """
    )
    @ApiResponse(responseCode = "200", description = "검증 성공",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "검증 실패(코드 불일치/만료/시도 초과)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "인증 요청 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/verify")
    public ResponseEntity<MessageResponse> verify(
            @RequestBody(description = "코드 검증 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = VerifyCode.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody VerifyCode dto) {

        signupService.verify(dto.email(), dto.code());
        return ResponseEntity.ok(new MessageResponse("회원가입이 완료되었습니다."));
    }
}