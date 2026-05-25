package com.assetbox.request.service;

import com.assetbox.request.domain.RequestPost;
import com.assetbox.request.domain.RequestStatus;
import com.assetbox.request.dto.RequestCreateRequest;
import com.assetbox.request.dto.RequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RequestPostService {

    RequestPost requireExists(Long id);

    RequestResponse get(Long id);

    RequestResponse create(Long requesterId, RequestCreateRequest request);

    RequestResponse update(Long id, Long requesterId, RequestCreateRequest request);

    RequestResponse assign(Long id, Long currentUserId);

    RequestResponse reject(Long id, Long currentUserId, String reason);

    RequestResponse reopen(Long id, Long requesterId, RequestStatus targetStatus);

    RequestResponse completeByLinkedPost(Long id, Long assigneeId, Long postId);

    void softDelete(Long id, Long requesterId);

    Page<RequestResponse> search(Pageable pageable);
}
