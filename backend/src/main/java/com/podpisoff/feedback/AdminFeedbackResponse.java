package com.podpisoff.feedback;

import java.time.Instant;

public record AdminFeedbackResponse(
    Long id,
    Long userId,
    String username,
    String message,
    FeedbackKind kind,
    Instant createdAt,
    String adminReply,
    Instant adminRepliedAt
) {
}
