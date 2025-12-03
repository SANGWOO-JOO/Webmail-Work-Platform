package dsn.webmail.service;

import dsn.webmail.entity.TechKeyword;
import dsn.webmail.repository.TechKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeywordExtractorService {

    private final TechKeywordRepository keywordRepository;

    /**
     * 텍스트에서 기술 키워드를 추출합니다.
     */
    public List<TechKeyword> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String lowerText = text.toLowerCase();
        List<TechKeyword> allKeywords = keywordRepository.findAll();

        List<TechKeyword> extracted = allKeywords.stream()
                .filter(k -> containsKeyword(lowerText, k.getKeyword()))
                .toList();

        log.debug("Extracted {} keywords from text", extracted.size());
        return extracted;
    }

    /**
     * 단어 경계를 고려한 키워드 매칭
     */
    private boolean containsKeyword(String text, String keyword) {
        // 특수문자가 포함된 키워드 처리 (예: "C++", "Node.js")
        String escapedKeyword = Pattern.quote(keyword.toLowerCase());
        String pattern = "(?i)\\b" + escapedKeyword + "\\b";

        try {
            return Pattern.compile(pattern).matcher(text).find();
        } catch (Exception e) {
            // 정규식 실패 시 단순 포함 검사
            return text.contains(keyword.toLowerCase());
        }
    }
}
