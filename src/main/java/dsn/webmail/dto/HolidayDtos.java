package dsn.webmail.dto;

import java.util.List;

public class HolidayDtos {

    public record HolidayResponse(
            String date,       // "2025-01-01" 형식
            String name,       // 공휴일명 (예: "신정", "설날")
            boolean isHoliday  // 공휴일 여부
    ) {}

    public record HolidayListResponse(
            List<HolidayResponse> holidays
    ) {}
}
