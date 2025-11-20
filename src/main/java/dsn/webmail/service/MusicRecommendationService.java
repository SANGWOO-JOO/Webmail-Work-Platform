package dsn.webmail.service;

import dsn.webmail.dto.MoodProfile;
import dsn.webmail.dto.MusicRecommendationResponse;
import dsn.webmail.dto.MusicRecommendationResponse.MoodAnalysis;
import dsn.webmail.dto.MusicRecommendationResponse.TrackInfo;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.ProcessedMail;
import dsn.webmail.repository.ProcessedMailRepository;
import dsn.webmail.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MusicRecommendationService {

    private final MusicMoodAnalyzer musicMoodAnalyzer;
    private final SpotifyApiClient spotifyApiClient;
    private final ProcessedMailRepository processedMailRepository;
    private final UserRepository userRepository;

    /**
     * 프롬프트 기반 음악 추천
     */
    public MusicRecommendationResponse recommendByPrompt(String prompt, int limit) {
        log.info("음악 추천 요청: {}", prompt);

        // AI로 기분 분석
        MoodProfile moodProfile = musicMoodAnalyzer.analyzeMood(prompt);
        log.debug("AI 분석 결과: {}", moodProfile);

        // Spotify 추천 요청
        List<TrackInfo> tracks = getRecommendedTracks(moodProfile, limit);

        // 응답 생성
        MoodAnalysis analysis = new MoodAnalysis(
                extractMood(moodProfile.reasoning()),
                moodProfile.energy(),
                moodProfile.valence(),
                moodProfile.targetTempo(),
                moodProfile.reasoning()
        );

        return new MusicRecommendationResponse(analysis, tracks);
    }

    /**
     * 오늘 받은 메일 기반 음악 추천
     */
    public MusicRecommendationResponse recommendByTodayMails(String email, int limit) {
        log.info("메일 기반 음악 추천 요청: email={}", email);

        // 사용자 조회
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        // 오늘 받은 메일 조회
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<ProcessedMail> todayMails = processedMailRepository
                .findByUserIdAndProcessedAtAfterOrderByProcessedAtDesc(user.getId(), todayStart);

        if (todayMails.isEmpty()) {
            // 메일이 없으면 기본 추천
            return recommendByPrompt("하루를 마무리하는 편안한 음악", limit);
        }

        // 메일 요약 생성
        String mailSummaries = todayMails.stream()
                .map(mail -> String.format("- [%s] %s: %s",
                        mail.getCategory() != null ? mail.getCategory().getDisplayName() : "미분류",
                        mail.getSubject(),
                        mail.getSummary() != null ? mail.getSummary() : ""))
                .collect(Collectors.joining("\n"));

        // AI로 메일 분석 기반 추천
        MoodProfile moodProfile = musicMoodAnalyzer.analyzeMailsAndRecommend(mailSummaries);
        log.debug("메일 기반 AI 분석 결과: {}", moodProfile);

        // Spotify 추천 요청
        List<TrackInfo> tracks = getRecommendedTracks(moodProfile, limit);

        // 응답 생성
        MoodAnalysis analysis = new MoodAnalysis(
                extractMood(moodProfile.reasoning()),
                moodProfile.energy(),
                moodProfile.valence(),
                moodProfile.targetTempo(),
                moodProfile.reasoning()
        );

        return new MusicRecommendationResponse(analysis, tracks);
    }

    private List<TrackInfo> getRecommendedTracks(MoodProfile moodProfile, int limit) {
        try {
            Map<String, Object> response = spotifyApiClient.getRecommendations(
                    moodProfile.genres().stream().limit(5).toList(),
                    moodProfile.energy(),
                    moodProfile.valence(),
                    moodProfile.targetTempo(),
                    moodProfile.acousticness(),
                    limit
            );

            List<Map<String, Object>> tracks = (List<Map<String, Object>>) response.get("tracks");

            return tracks.stream()
                    .map(this::mapToTrackInfo)
                    .toList();
        } catch (Exception e) {
            log.error("Spotify 추천 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private TrackInfo mapToTrackInfo(Map<String, Object> track) {
        String id = (String) track.get("id");
        String name = (String) track.get("name");

        // 아티스트 정보
        List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
        String artist = artists.stream()
                .map(a -> (String) a.get("name"))
                .collect(Collectors.joining(", "));

        // 앨범 정보
        Map<String, Object> album = (Map<String, Object>) track.get("album");
        String albumName = (String) album.get("name");

        // 앨범 이미지
        List<Map<String, Object>> images = (List<Map<String, Object>>) album.get("images");
        String albumImageUrl = images.isEmpty() ? null : (String) images.get(0).get("url");

        // 미리듣기 URL
        String previewUrl = (String) track.get("preview_url");

        // Spotify URL
        Map<String, Object> externalUrls = (Map<String, Object>) track.get("external_urls");
        String spotifyUrl = (String) externalUrls.get("spotify");

        return new TrackInfo(id, name, artist, albumName, albumImageUrl, previewUrl, spotifyUrl);
    }

    private String extractMood(String reasoning) {
        // 간단한 키워드 기반 기분 추출
        if (reasoning.contains("행복") || reasoning.contains("밝은") || reasoning.contains("신나")) {
            return "행복";
        } else if (reasoning.contains("슬픔") || reasoning.contains("우울") || reasoning.contains("위로")) {
            return "위로";
        } else if (reasoning.contains("에너지") || reasoning.contains("활력") || reasoning.contains("신남")) {
            return "에너지";
        } else if (reasoning.contains("편안") || reasoning.contains("휴식") || reasoning.contains("잔잔")) {
            return "휴식";
        } else if (reasoning.contains("집중") || reasoning.contains("작업")) {
            return "집중";
        }
        return "추천";
    }
}
