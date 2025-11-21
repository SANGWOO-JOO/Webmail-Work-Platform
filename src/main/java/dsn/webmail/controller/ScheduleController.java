package dsn.webmail.controller;

import dsn.webmail.dto.HolidayDtos.HolidayListResponse;
import dsn.webmail.dto.ScheduleDtos.CreateEventRequest;
import dsn.webmail.dto.ScheduleDtos.ScheduleEventResponse;
import dsn.webmail.dto.ScheduleDtos.ScheduleListResponse;
import dsn.webmail.dto.ScheduleDtos.UpdateEventRequest;
import dsn.webmail.security.JwtTokenProvider;
import dsn.webmail.service.HolidayService;
import dsn.webmail.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 일정 Controller
 * - View: Thymeleaf 템플릿 반환
 * - API: JSON 반환 (REST API)
 */
@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final HolidayService holidayService;
    private final JwtTokenProvider jwtTokenProvider;

    // ========== View 엔드포인트 ==========

    /**
     * 일정 메인 페이지
     * HTML 페이지는 인증 없이 접근 가능 (auth-guard.js가 클라이언트에서 검증)
     */
    @GetMapping
    public String schedule() {
        return "schedule/index";
    }

    // ========== API 엔드포인트 ==========

    /**
     * 월별 일정 목록 조회 API
     * JWT 인증 필요
     */
    @GetMapping("/api/events")
    @ResponseBody
    public ResponseEntity<ScheduleListResponse> getEvents(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        String token = authorization.replace("Bearer ", "");
        String email = jwtTokenProvider.getEmailFromToken(token);
        ScheduleListResponse events = scheduleService.getScheduleByMonth(email, year, month);
        return ResponseEntity.ok(events);
    }

    /**
     * 일정 상세 조회 API
     * JWT 인증 필요
     */
    @GetMapping("/api/events/{id}")
    @ResponseBody
    public ResponseEntity<ScheduleEventResponse> getEventById(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        String token = authorization.replace("Bearer ", "");
        String email = jwtTokenProvider.getEmailFromToken(token);
        ScheduleEventResponse event = scheduleService.getScheduleById(email, id);
        return ResponseEntity.ok(event);
    }

    /**
     * 일정 생성 API
     * JWT 인증 필요
     */
    @PostMapping("/api/events")
    @ResponseBody
    public ResponseEntity<ScheduleEventResponse> createEvent(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreateEventRequest request) {
        String token = authorization.replace("Bearer ", "");
        String email = jwtTokenProvider.getEmailFromToken(token);
        ScheduleEventResponse event = scheduleService.createSchedule(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    /**
     * 일정 수정 API
     * JWT 인증 필요
     */
    @PutMapping("/api/events/{id}")
    @ResponseBody
    public ResponseEntity<ScheduleEventResponse> updateEvent(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        String token = authorization.replace("Bearer ", "");
        String email = jwtTokenProvider.getEmailFromToken(token);
        ScheduleEventResponse event = scheduleService.updateSchedule(email, id, request);
        return ResponseEntity.ok(event);
    }

    /**
     * 일정 삭제 API
     * JWT 인증 필요
     */
    @DeleteMapping("/api/events/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteEvent(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        String token = authorization.replace("Bearer ", "");
        String email = jwtTokenProvider.getEmailFromToken(token);
        scheduleService.deleteSchedule(email, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 월별 공휴일 조회 API
     * 인증 불필요 (공개 API)
     */
    @GetMapping("/api/holidays")
    @ResponseBody
    public ResponseEntity<HolidayListResponse> getHolidays(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }
        if (month == null) {
            month = java.time.LocalDate.now().getMonthValue();
        }
        HolidayListResponse holidays = holidayService.getHolidaysByMonth(year, month);
        return ResponseEntity.ok(holidays);
    }
}
