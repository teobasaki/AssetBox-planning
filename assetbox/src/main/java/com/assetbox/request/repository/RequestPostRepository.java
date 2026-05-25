package com.assetbox.request.repository;

import com.assetbox.request.domain.RequestPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestPostRepository extends JpaRepository<RequestPost, Long> {
}
