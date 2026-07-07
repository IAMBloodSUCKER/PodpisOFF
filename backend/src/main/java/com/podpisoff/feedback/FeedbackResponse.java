package com.podpisoff.feedback;

import java.time.Instant;

public record FeedbackResponse(
    Long id,
    String message,
    Instant createdAt,
    String adminReply,
    Instant adminRepliedAt
) {
}
