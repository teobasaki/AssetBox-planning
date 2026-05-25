package com.assetbox.message.dto;

import java.time.LocalDateTime;

public record ConversationSummary(
        Long partnerId,
        String partnerNickname,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
}
