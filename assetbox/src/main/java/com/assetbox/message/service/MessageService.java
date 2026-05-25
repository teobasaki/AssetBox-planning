package com.assetbox.message.service;

import com.assetbox.message.dto.ConversationSummary;
import com.assetbox.message.dto.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageService {

    MessageResponse send(Long fromUserId, Long toUserId, String content);

    Page<MessageResponse> conversation(Long meId, Long partnerId, Pageable pageable);

    Page<ConversationSummary> inbox(Long meId, Pageable pageable);

    long unreadCount(Long meId);

    void markRead(Long meId, Long partnerId);
}
