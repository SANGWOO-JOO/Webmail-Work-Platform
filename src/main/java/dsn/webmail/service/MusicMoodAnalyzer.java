package dsn.webmail.service;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dsn.webmail.dto.MoodProfile;

public interface MusicMoodAnalyzer {

    @UserMessage("""
        사용자의 현재 기분이나 상황을 분석하여 적합한 음악 특성을 추천해주세요.

        사용자 입력: {{prompt}}

        Spotify의 오디오 특성 기준:
        - energy (0.0-1.0): 곡의 에너지/강도. 1.0에 가까울수록 빠르고 강렬함
        - valence (0.0-1.0): 긍정성/행복감. 1.0에 가까울수록 밝고 행복한 느낌
        - targetTempo (BPM): 곡의 속도. 60-80 느림, 100-120 중간, 130+ 빠름
        - acousticness (0.0-1.0): 어쿠스틱 정도. 1.0에 가까울수록 어쿠스틱 악기 위주

        사용 가능한 장르 (최대 5개 선택):
        pop, rock, hip-hop, r-n-b, electronic, dance, classical, jazz,
        acoustic, indie, k-pop, ambient, chill, study, sleep, work-out,
        party, romance, sad, happy

        사용자의 감정과 상황을 파악하여 가장 적합한 음악 특성을 추천해주세요.
        """)
    MoodProfile analyzeMood(@V("prompt") String prompt);

    @UserMessage("""
        오늘 사용자가 받은 메일들의 내용을 분석하여 퇴근 후 들으면 좋을 음악을 추천해주세요.

        오늘 받은 메일 요약:
        {{mailSummaries}}

        메일 내용을 분석하여 사용자의 하루 상태를 추정하고,
        스트레스 해소나 휴식에 도움이 될 음악 특성을 추천해주세요.

        예시:
        - 업무 요청이 많았다면: 편안한 음악으로 스트레스 해소
        - 긍정적인 내용이 많았다면: 기분을 유지할 수 있는 밝은 음악
        - 회의가 많았다면: 조용하고 잔잔한 음악으로 휴식
        """)
    MoodProfile analyzeMailsAndRecommend(@V("mailSummaries") String mailSummaries);
}
