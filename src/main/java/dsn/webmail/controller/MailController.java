package dsn.webmail.controller;

import dsn.webmail.dto.MailDtos.MailDetailResponse;
import dsn.webmail.dto.MailDtos.MailListResponse;
import dsn.webmail.dto.ReplyDtos.ReplyDraftResponse;
import dsn.webmail.dto.ReplyDtos.ReplyGenerationRequest;
import dsn.webmail.entity.MailCategory;
import dsn.webmail.security.JwtTokenProvider;
import dsn.webmail.service.MailListService;
import dsn.webmail.service.ReplyGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 메일 Controller
 * - View: Thymeleaf 템플릿 반환
 * - API: JSON 반환 (REST API)
 */
@Controller
@RequestMapping("/dashboard/mail")
@RequiredArgsConstructor
public class MailController {

    private final MailListService mailListService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ReplyGenerationService replyGenerationService;

    // ========== View 엔드포인트 ==========

    /**
     * 메일 목록 페이지
     */
    @GetMapping
    public String mailList() {
        return "mail/index";
    }

    // ========== API 엔드포인트 ==========

    /**
     * 메일 목록 조회 API
     * JWT 인증 필요
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<MailListResponse> getMailList(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) MailCategory category) {
        String token = authorization.replace("Bearer ", "");
        String email = jwtTokenProvider.getEmailFromToken(token);
        MailListResponse response = mailListService.getMailList(email, page, size, category);
        return ResponseEntity.ok(response);
    }

    /**
     * 메일 상세 조회 API
     * JWT 인증 필요
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<MailDetailResponse> getMailDetail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        String token = authorization.replace("Bearer ", "");
        String email = jwtTokenProvider.getEmailFromToken(token);
        MailDetailResponse response = mailListService.getMailDetail(email, id);
        return ResponseEntity.ok(response);
    }

    /**
     * 메일 재분석 API
     * JWT 인증 필요
     */
    @PostMapping("/api/{id}/analyze")
    @ResponseBody
    public ResponseEntity<MailDetailResponse> reanalyzeMail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        String token = authorization.replace("Bearer ", "");
        String email = jwtTokenProvider.getEmailFromToken(token);
        MailDetailResponse response = mailListService.reanalyzeMail(email, id);
        return ResponseEntity.ok(response);
    }

    /**
     * 답장 초안 생성 API
     * JWT 인증 필요
     */
    @PostMapping("/api/{id}/generate-reply")
    @ResponseBody
    public ResponseEntity<ReplyDraftResponse> generateReply(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @RequestBody ReplyGenerationRequest request) {
        String token = authorization.replace("Bearer ", "");
        String email = jwtTokenProvider.getEmailFromToken(token);
        ReplyDraftResponse response = replyGenerationService.generateReply(email, id, request);
        return ResponseEntity.ok(response);
    }
}
