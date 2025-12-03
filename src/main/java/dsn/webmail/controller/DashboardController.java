package dsn.webmail.controller;

import dsn.webmail.dto.DashboardDtos.AccountInfoResponse;
import dsn.webmail.dto.DashboardDtos.DashboardStatsResponse;
import dsn.webmail.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 대시보드 Controller
 * - View: Thymeleaf 템플릿 반환
 * - API: JSON 반환 (REST API)
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ========== View 엔드포인트 ==========

    /**
     * 대시보드 메인 페이지
     * HTML 페이지는 인증 없이 접근 가능 (auth-guard.js가 클라이언트에서 검증)
     */
    @GetMapping
    public String dashboard() {
        return "dashboard/index";
    }

    // ========== API 엔드포인트 ==========

    /**
     * 대시보드 통계 조회 API
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<DashboardStatsResponse> getStats(
            @AuthenticationPrincipal String email) {
        DashboardStatsResponse stats = dashboardService.getDashboardStats(email);
        return ResponseEntity.ok(stats);
    }

    /**
     * 계정 정보 조회 API
     */
    @GetMapping("/api/account-info")
    @ResponseBody
    public ResponseEntity<AccountInfoResponse> getAccountInfo(
            @AuthenticationPrincipal String email) {
        AccountInfoResponse accountInfo = dashboardService.getAccountInfo(email);
        return ResponseEntity.ok(accountInfo);
    }
}
