package app.dto;

import app.model.enums.MediaType;
import app.model.enums.ageRestriction;

public record MediaDto (
    String id,
    String title,
    MediaType type,
    Integer releaseYear,
    ageRestriction fsk,
    String createdByUserId,
    String createdAt
) {}