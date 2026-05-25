package com.assetbox.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        Long authorId,
        String authorNickname,
        String content,
        boolean deleted,
        Long parentId,
        List<CommentResponse> replies,
        LocalDateTime createdAt
) {
}
