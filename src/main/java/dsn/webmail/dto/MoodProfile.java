package dsn.webmail.dto;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public record MoodProfile(
    @Description("에너지 레벨 (0.0-1.0)")
    float energy,

    @Description("긍정성/행복감 (0.0-1.0)")
    float valence,

    @Description("목표 템포 BPM (60-180)")
    int targetTempo,

    @Description("어쿠스틱 선호도 (0.0-1.0)")
    float acousticness,

    @Description("추천 장르 목록 (최대 5개)")
    List<String> genres,

    @Description("사용자가 언급한 아티스트 이름 목록")
    List<String> artists,

    @Description("추천 이유 설명")
    String reasoning
) {}
