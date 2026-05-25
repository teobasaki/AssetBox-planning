package com.assetbox.request.repository;

import com.assetbox.request.domain.RequestComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestCommentRepository extends JpaRepository<RequestComment, Long> {
}
