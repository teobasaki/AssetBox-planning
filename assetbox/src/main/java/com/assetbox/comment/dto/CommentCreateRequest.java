package com.assetbox.comment.dto;

public record CommentCreateRequest(String content, Long parentId) {
}
