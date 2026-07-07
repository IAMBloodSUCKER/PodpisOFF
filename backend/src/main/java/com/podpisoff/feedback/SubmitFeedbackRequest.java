package com.podpisoff.feedback;

public record SubmitFeedbackRequest(String message, FeedbackKind kind) {
    public SubmitFeedbackRequest {
        if (kind == null) {
            kind = FeedbackKind.FEEDBACK;
        }
    }
}
