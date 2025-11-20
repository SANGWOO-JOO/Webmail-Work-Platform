package dsn.webmail.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String title;
    private String dateTime;
    private String location;
    private Float confidence;
}
