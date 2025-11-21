package dsn.webmail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ScheduleDtos {

    public record ScheduleEventResponse(
            Long id,
            String title,
            String dateTime,
            String location,
            Float confidence,
            String sourceMessageId,
            String mailSubject,
            String mailFromAddress,
            // 새로 추가된 필드
            Boolean isManual,
            String description,
            String category,
            String priority,
            String relatedLink,
            String color
    ) {}

    public record ScheduleListResponse(
            List<ScheduleEventResponse> events
    ) {}

    // 일정 생성 요청 DTO
    public record CreateEventRequest(
            @NotBlank(message = "제목은 필수입니다")
            @Size(max = 100, message = "제목은 100자 이내여야 합니다")
            String title,

            @NotBlank(message = "날짜/시간은 필수입니다")
            @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}", message = "날짜 형식은 yyyy-MM-dd HH:mm 이어야 합니다")
            String dateTime,

            @Size(max = 500, message = "설명은 500자 이내여야 합니다")
            String description,

            String category,  // 개발, 회의, 배포, 리뷰, 학습, 기타

            String priority,  // HIGH, MEDIUM, LOW

            String relatedLink,  // GitHub, Jira 링크

            String color  // HEX 색상 코드 (예: #3182F6)
    ) {}

    // 일정 수정 요청 DTO
    public record UpdateEventRequest(
            @NotBlank(message = "제목은 필수입니다")
            @Size(max = 100, message = "제목은 100자 이내여야 합니다")
            String title,

            @NotBlank(message = "날짜/시간은 필수입니다")
            @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}", message = "날짜 형식은 yyyy-MM-dd HH:mm 이어야 합니다")
            String dateTime,

            @Size(max = 500, message = "설명은 500자 이내여야 합니다")
            String description,

            String category,

            String priority,

            String relatedLink,

            String color
    ) {}
}
