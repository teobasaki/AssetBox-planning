package com.assetbox.post.dto;

import java.util.List;

public record PostSummaryResponse(
        Long id,
        String title,
        Long authorId,
        Long categoryId,
        List<String> tags,
        Long linkedRequestId,
        long viewCount,
        long likeCount,
        long commentCount
) {
}
