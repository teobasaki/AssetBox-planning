package com.assetbox.file.dto;

import com.assetbox.file.domain.FilePurpose;

public record FileResponse(
        Long id,
        String originalName,
        String extension,
        long sizeBytes,
        FilePurpose purpose,
        Long ownerId,
        Long uploadedBy,
        String contentType
) {
}
