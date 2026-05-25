package com.assetbox.feedback.dto;

import com.assetbox.feedback.domain.FeedbackStatus;
import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        String title,
        String content,
        Long userId,
        String userNickname,
        FeedbackStatus status,
        LocalDateTime createdAt
) {
}
