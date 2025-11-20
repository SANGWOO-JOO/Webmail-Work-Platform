package dsn.webmail.dto;

import java.util.List;

public record MusicRecommendationResponse(
    MoodAnalysis analysis,
    List<TrackInfo> tracks
) {
    public record MoodAnalysis(
        String mood,
        float energy,
        float valence,
        int tempo,
        String reasoning
    ) {}

    public record TrackInfo(
        String id,
        String name,
        String artist,
        String album,
        String albumImageUrl,
        String previewUrl,
        String spotifyUrl
    ) {}
}
