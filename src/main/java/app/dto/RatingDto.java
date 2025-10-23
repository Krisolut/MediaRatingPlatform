package app.dto;

public record RatingDto (
        String id,
        String mediaId,
        String userId,
        Integer stars,
        String comment,
        String createdAt
)
{}