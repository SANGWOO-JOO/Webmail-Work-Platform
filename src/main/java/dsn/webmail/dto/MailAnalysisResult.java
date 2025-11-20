package dsn.webmail.dto;

import dev.langchain4j.model.output.structured.Description;
import dsn.webmail.entity.MailCategory;

public record MailAnalysisResult(
    @Description("메일 카테고리: ANNOUNCEMENT(공지), NOTION_ALERT(노션 알림), WORK_REQUEST(업무 요청), INFORMATION(내용 전달), INQUIRY(질의 사항), UNKNOWN(미분류)")
    MailCategory category,

    @Description("분류 신뢰도 (0.0-1.0)")
    float confidence,

    @Description("5줄 요약 (각 줄은 번호로 시작, 줄바꿈으로 구분)")
    String summary
) {}
