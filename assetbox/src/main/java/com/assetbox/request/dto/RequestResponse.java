package com.assetbox.request.dto;

import com.assetbox.request.domain.RequestStatus;
import java.time.LocalDate;

public record RequestResponse(
        Long id,
        String title,
        String content,
        String assetType,
        String preferredStyle,
        String engine,
        LocalDate deadline,
        RequestStatus status,
        Long requesterId,
        Long assigneeId,
        Long linkedPostId,
        Long referenceFileId
) {
}
