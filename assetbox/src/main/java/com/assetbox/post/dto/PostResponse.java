package com.assetbox.post.dto;

import java.util.List;

public record PostResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        Long categoryId,
        List<String> tags,
        Long linkedRequestId,
        long viewCount,
        long likeCount
) {
}
