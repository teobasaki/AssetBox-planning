package com.assetbox.post.service;

import com.assetbox.post.domain.Post;
import com.assetbox.post.dto.PostCreateRequest;
import com.assetbox.post.dto.PostResponse;
import com.assetbox.post.dto.PostSummaryResponse;
import com.assetbox.post.dto.PostUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface PostService {

    Post requireExists(Long postId);

    PostResponse get(Long postId, Long requesterId);

    PostResponse create(Long authorId, PostCreateRequest request, List<MultipartFile> files, MultipartFile thumbnail);

    PostResponse update(Long postId, Long requesterId, PostUpdateRequest request);

    void softDelete(Long postId, Long requesterId);

    Page<PostSummaryResponse> search(Pageable pageable);
}
