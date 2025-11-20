package dsn.webmail.service;

import dsn.webmail.dto.ScheduleDtos.ScheduleEventResponse;
import dsn.webmail.dto.ScheduleDtos.ScheduleListResponse;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.MailEvent;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.repository.MailEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleService {

    private final AppUserRepository appUserRepository;
    private final MailEventRepository mailEventRepository;

    @Transactional(readOnly = true)
    public ScheduleListResponse getScheduleByMonth(String email, Integer year, Integer month) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        // 기본값: 현재 연/월
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }

        // "2025-11" 형식으로 조회
        String yearMonth = String.format("%d-%02d", year, month);
        List<MailEvent> events = mailEventRepository.findByUserIdAndYearMonth(user.getId(), yearMonth);

        List<ScheduleEventResponse> eventResponses = events.stream()
                .map(this::toEventResponse)
                .toList();

        return new ScheduleListResponse(eventResponses);
    }

    @Transactional(readOnly = true)
    public ScheduleEventResponse getScheduleById(String email, Long eventId) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        MailEvent event = mailEventRepository.findByIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다: " + eventId));

        return toEventResponse(event);
    }

    @Transactional
    public void deleteSchedule(String email, Long eventId) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        MailEvent event = mailEventRepository.findByIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다: " + eventId));

        mailEventRepository.delete(event);
        log.info("일정 삭제: userId={}, eventId={}", user.getId(), eventId);
    }

    private ScheduleEventResponse toEventResponse(MailEvent event) {
        return new ScheduleEventResponse(
                event.getId(),
                event.getTitle(),
                event.getDateTime(),
                event.getLocation(),
                event.getConfidence(),
                event.getSourceMessageId()
        );
    }
}
