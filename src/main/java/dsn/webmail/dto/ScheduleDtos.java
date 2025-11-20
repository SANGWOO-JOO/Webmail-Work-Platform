package dsn.webmail.dto;

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
            String mailFromAddress
    ) {}

    public record ScheduleListResponse(
            List<ScheduleEventResponse> events
    ) {}
}
