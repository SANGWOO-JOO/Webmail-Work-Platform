package dsn.webmail.service;

import dsn.webmail.dto.ScheduleDtos.CreateEventRequest;
import dsn.webmail.dto.ScheduleDtos.ScheduleEventResponse;
import dsn.webmail.dto.ScheduleDtos.ScheduleListResponse;
import dsn.webmail.dto.ScheduleDtos.UpdateEventRequest;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.MailEvent;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.repository.EventKeywordRepository;
import dsn.webmail.repository.MailEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleService {

    private final AppUserRepository appUserRepository;
    private final MailEventRepository mailEventRepository;
    private final EventKeywordRepository eventKeywordRepository;
    private final EventKeywordExtractorService eventKeywordExtractorService;

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

        // 연결된 EventKeyword 먼저 삭제
        eventKeywordRepository.deleteByEvent(event);

        mailEventRepository.delete(event);
        log.info("일정 삭제: userId={}, eventId={}", user.getId(), eventId);
    }

    /**
     * 일정 생성 (수동)
     */
    @Transactional
    public ScheduleEventResponse createSchedule(String email, CreateEventRequest request) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        MailEvent event = MailEvent.builder()
                .user(user)
                .title(request.title())
                .dateTime(request.dateTime())
                .description(request.description())
                .category(request.category())
                .priority(request.priority())
                .relatedLink(request.relatedLink())
                .color(request.color() != null ? request.color() : "#3182F6")
                .isManual(true)
                .createdAt(LocalDateTime.now())
                .build();

        MailEvent savedEvent = mailEventRepository.save(event);
        log.info("일정 생성: userId={}, eventId={}, title={}", user.getId(), savedEvent.getId(), savedEvent.getTitle());

        // 기술 키워드 추출 (비동기적으로 처리)
        try {
            eventKeywordExtractorService.extractAndSaveKeywords(savedEvent);
        } catch (Exception e) {
            log.warn("일정 키워드 추출 실패: eventId={}, error={}", savedEvent.getId(), e.getMessage());
        }

        return toEventResponse(savedEvent);
    }

    /**
     * 일정 수정
     */
    @Transactional
    public ScheduleEventResponse updateSchedule(String email, Long eventId, UpdateEventRequest request) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        MailEvent event = mailEventRepository.findByIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다: " + eventId));

        event.setTitle(request.title());
        event.setDateTime(request.dateTime());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setPriority(request.priority());
        event.setRelatedLink(request.relatedLink());
        event.setColor(request.color());
        event.setUpdatedAt(LocalDateTime.now());

        MailEvent updatedEvent = mailEventRepository.save(event);
        log.info("일정 수정: userId={}, eventId={}, title={}", user.getId(), updatedEvent.getId(), updatedEvent.getTitle());

        // 기술 키워드 재추출
        try {
            eventKeywordExtractorService.extractAndSaveKeywords(updatedEvent);
        } catch (Exception e) {
            log.warn("일정 키워드 추출 실패: eventId={}, error={}", updatedEvent.getId(), e.getMessage());
        }

        return toEventResponse(updatedEvent);
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
                mailFromAddress,
                event.getIsManual(),
                event.getDescription(),
                event.getCategory(),
                event.getPriority(),
                event.getRelatedLink(),
                event.getColor()
        );
    }

    private ScheduleEventResponse toEventResponse(MailEvent event) {
        return new ScheduleEventResponse(
                event.getId(),
                event.getTitle(),
                event.getDateTime(),
                event.getLocation(),
                event.getConfidence(),
                event.getSourceMessageId(),
                null,  // mailSubject
                null,  // mailFromAddress
                event.getIsManual(),
                event.getDescription(),
                event.getCategory(),
                event.getPriority(),
                event.getRelatedLink(),
                event.getColor()
        );
    }
}
