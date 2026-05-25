package com.assetbox.comment.service;

import com.assetbox.comment.domain.Comment;
import com.assetbox.comment.dto.CommentCreateRequest;
import com.assetbox.comment.dto.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    Comment requireExists(Long commentId);

    Page<CommentResponse> listByPost(Long postId, Pageable pageable);

    CommentResponse create(Long postId, Long authorId, CommentCreateRequest request);

    void softDelete(Long commentId, Long requesterId);
}
