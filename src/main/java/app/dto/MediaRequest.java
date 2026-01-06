package app.dto;

import app.model.enums.AgeRestriction;

import java.util.List;

public record MediaRequest(
        String title,
        String description,
        String mediaType,
        Integer releaseYear,
        AgeRestriction ageRestriction,
        List<String> genres
) {}