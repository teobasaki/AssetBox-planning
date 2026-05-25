package com.assetbox.message.service;

import com.assetbox.message.dto.MessageResponse;

public interface MessageWsBroker {

    void pushMessage(Long receiverId, MessageResponse message);

    void pushUnreadCount(Long receiverId, long count);
}
