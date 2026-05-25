package com.assetbox.message.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        String senderNickname,
        Long receiverId,
        String receiverNickname,
        String content,
        boolean read,
        LocalDateTime createdAt
) {
}
