package com.assetbox.message.dto;

public record MessageSendRequest(Long toUserId, String content) {
}
