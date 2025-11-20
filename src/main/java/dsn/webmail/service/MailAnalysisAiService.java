package dsn.webmail.service;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dsn.webmail.dto.MailAnalysisResult;

public interface MailAnalysisAiService {

    @UserMessage("""
        다음 이메일을 분석하여 카테고리를 분류하고 5줄로 요약해주세요.

        제목: {{subject}}
        발신자: {{from}}
        내용: {{content}}

        카테고리 옵션:
        - ANNOUNCEMENT: 전사/팀 공지사항, 시스템 안내
        - NOTION_ALERT: Notion 페이지 업데이트, 댓글 알림
        - WORK_REQUEST: 업무 협조, 자료 요청, 검토 요청
        - INFORMATION: 정보 공유, 회의록, 참고 자료 전달
        - INQUIRY: 질문, 문의, 확인 요청
        - UNKNOWN: 위 카테고리에 해당하지 않는 경우

        요약은 핵심 내용만 5줄로 작성해주세요. 각 줄은 "1. ", "2. " 형식으로 시작합니다.
        """)
    MailAnalysisResult analyze(
        @V("subject") String subject,
        @V("from") String from,
        @V("content") String content
    );
}
