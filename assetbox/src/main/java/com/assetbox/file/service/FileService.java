package com.assetbox.file.service;

import com.assetbox.file.domain.FilePurpose;
import com.assetbox.file.domain.StoredFile;
import com.assetbox.file.dto.FileResponse;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    StoredFile save(FilePurpose purpose, Long ownerId, Long uploadedBy, MultipartFile file);

    List<StoredFile> saveAll(FilePurpose purpose, Long ownerId, Long uploadedBy, List<MultipartFile> files);

    Resource load(Long fileId, Long requesterId);

    FileResponse meta(Long fileId, Long requesterId);

    void delete(Long fileId, Long requesterId);
}
