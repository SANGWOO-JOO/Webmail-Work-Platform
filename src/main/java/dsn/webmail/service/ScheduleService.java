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

        // "2025-11" 형식으로 조회 (ProcessedMail JOIN)
        String yearMonth = String.format("%d-%02d", year, month);
        List<Object[]> results = mailEventRepository.findByUserIdAndYearMonthWithMailInfo(user.getId(), yearMonth);

        List<ScheduleEventResponse> eventResponses = results.stream()
                .map(this::toEventResponseWithMailInfo)
                .toList();

        return new ScheduleListResponse(eventResponses);
    }

    @Transactional(readOnly = true)
    public ScheduleEventResponse getScheduleById(String email, Long eventId) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        List<Object[]> results = mailEventRepository.findByIdAndUserIdWithMailInfo(eventId, user.getId());

        if (results.isEmpty()) {
            throw new RuntimeException("일정을 찾을 수 없습니다: " + eventId);
        }

        return toEventResponseWithMailInfo(results.get(0));
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

    private ScheduleEventResponse toEventResponseWithMailInfo(Object[] result) {
        MailEvent event = (MailEvent) result[0];
        String mailSubject = (String) result[1];
        String mailFromAddress = (String) result[2];

        return new ScheduleEventResponse(
                event.getId(),
                event.getTitle(),
                event.getDateTime(),
                event.getLocation(),
                event.getConfidence(),
                event.getSourceMessageId(),
                mailSubject,
                mailFromAddress
        );
    }
}
