package app.dto;

public record UserDto (
    String id,
    String username,
    String displayName,
    String createdAt
) {}
