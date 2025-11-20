package dsn.webmail.service;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ReplyGenerationAiService {

    @UserMessage("""
            다음 이메일에 대한 답장 초안을 작성해주세요.

            [원본 메일]
            제목: {{subject}}
            발신자: {{from}}
            내용: {{content}}

            [답장 조건]
            톤: {{tone}}
            답장 유형: {{replyType}}
            추가 컨텍스트: {{additionalContext}}

            [작성 규칙]
            1. 원본 메일의 언어와 동일한 언어로 작성
            2. 비즈니스 이메일 형식 준수
            3. 인사말과 마무리 포함
            4. 원본 메일의 핵심 내용에 대응하는 답변 포함
            5. 발신자 이름은 [사용자 이름]으로 표시

            답장 초안만 출력하세요.
            """)
    String generateReply(
            @V("subject") String subject,
            @V("from") String from,
            @V("content") String content,
            @V("tone") String tone,
            @V("replyType") String replyType,
            @V("additionalContext") String additionalContext
    );
}
