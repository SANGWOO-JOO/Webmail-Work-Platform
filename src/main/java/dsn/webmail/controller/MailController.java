package dsn.webmail.controller;

import dsn.webmail.dto.MailDtos.MailDetailResponse;
import dsn.webmail.dto.MailDtos.MailListResponse;
import dsn.webmail.dto.MailDtos.MailReplyRequest;
import dsn.webmail.dto.MailDtos.MailReplyResponse;
import dsn.webmail.dto.ReplyDtos.ReplyDraftResponse;
import dsn.webmail.dto.ReplyDtos.ReplyGenerationRequest;
import dsn.webmail.entity.MailCategory;
import dsn.webmail.service.MailListService;
import dsn.webmail.service.MailSendService;
import dsn.webmail.service.ReplyGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final ReplyGenerationService replyGenerationService;
    private final MailSendService mailSendService;

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
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<MailListResponse> getMailList(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) MailCategory category) {
        MailListResponse response = mailListService.getMailList(email, page, size, category);
        return ResponseEntity.ok(response);
    }

    /**
     * 메일 상세 조회 API
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<MailDetailResponse> getMailDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {
        MailDetailResponse response = mailListService.getMailDetail(email, id);
        return ResponseEntity.ok(response);
    }

    /**
     * 메일 재분석 API
     */
    @PostMapping("/api/{id}/analyze")
    @ResponseBody
    public ResponseEntity<MailDetailResponse> reanalyzeMail(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {
        MailDetailResponse response = mailListService.reanalyzeMail(email, id);
        return ResponseEntity.ok(response);
    }

    /**
     * 답장 초안 생성 API
     */
    @PostMapping("/api/{id}/generate-reply")
    @ResponseBody
    public ResponseEntity<ReplyDraftResponse> generateReply(
            @AuthenticationPrincipal String email,
            @PathVariable Long id,
            @RequestBody ReplyGenerationRequest request) {
        ReplyDraftResponse response = replyGenerationService.generateReply(email, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 메일 답장 발송 API
     */
    @PostMapping("/api/reply")
    @ResponseBody
    public ResponseEntity<MailReplyResponse> sendReply(
            @AuthenticationPrincipal String email,
            @RequestBody MailReplyRequest request) {
        MailReplyResponse response = mailSendService.sendReply(email, request);
        return ResponseEntity.ok(response);
    }
}
