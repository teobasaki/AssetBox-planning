package com.assetbox.feedback.service;

import com.assetbox.feedback.dto.FeedbackCreateRequest;
import com.assetbox.feedback.dto.FeedbackResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {

    void create(Long userId, FeedbackCreateRequest request);

    Page<FeedbackResponse> list(Pageable pageable);

    FeedbackResponse markRead(Long id);

    long countNew();
}
