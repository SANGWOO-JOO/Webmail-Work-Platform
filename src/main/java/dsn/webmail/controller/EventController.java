package dsn.webmail.controller;


import dsn.webmail.dto.EventRequest;
import dsn.webmail.dto.EventResponse;
import dsn.webmail.entity.MailEvent;
import dsn.webmail.service.EventExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventExtractionService service;

    @PostMapping("/extract")
    public ResponseEntity<EventResponse> extractEvent(
            @RequestBody EventRequest request
    ) {
        // 서비스 호출
        MailEvent event = service.extractEventFromMail(request.getMailContent()
        );

        // 응답 생성
        EventResponse response = new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDateTime(),
                event.getLocation(),
                event.getConfidence()
        );

        return ResponseEntity.ok(response);
    }

}
